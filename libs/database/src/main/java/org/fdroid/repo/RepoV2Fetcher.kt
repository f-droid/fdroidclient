package org.fdroid.repo

import android.net.Uri
import java.io.File
import java.net.Proxy
import java.security.DigestInputStream
import java.security.MessageDigest
import mu.KotlinLogging
import org.fdroid.database.Repository
import org.fdroid.download.DownloadRequest
import org.fdroid.download.DownloaderFactory
import org.fdroid.download.HttpManager
import org.fdroid.download.getDigestInputStream
import org.fdroid.fdroid.getDigestHex
import org.fdroid.index.IndexParser
import org.fdroid.index.RepoUriBuilder
import org.fdroid.index.SigningException
import org.fdroid.index.TempFileProvider
import org.fdroid.index.parseEntry
import org.fdroid.index.v2.EntryVerifier
import org.fdroid.index.v2.FileV2
import org.fdroid.index.v2.IndexV2FullStreamProcessor
import org.fdroid.index.v2.SIGNED_FILE_NAME

internal class RepoV2Fetcher(
  private val tempFileProvider: TempFileProvider,
  private val downloaderFactory: DownloaderFactory,
  private val httpManager: HttpManager,
  private val repoUriBuilder: RepoUriBuilder,
  private val proxy: Proxy? = null,
) : RepoFetcher {
  private val log = KotlinLogging.logger {}

  @Throws(SigningException::class)
  override suspend fun fetchRepo(
    uri: Uri,
    repo: Repository,
    receiver: RepoPreviewReceiver,
    fingerprint: String?,
  ): File {
    // download and verify entry
    val entryFile = tempFileProvider.createTempFile(null)
    val entryDownloader =
      downloaderFactory.create(
        repo = repo,
        uri = repoUriBuilder.getUri(repo, SIGNED_FILE_NAME),
        indexFile = FileV2.fromPath("/$SIGNED_FILE_NAME"),
        destFile = entryFile,
      )
    val (cert, entry) =
      try {
        entryDownloader.download()
        val verifier = EntryVerifier(entryFile, null, fingerprint)
        verifier.getStreamAndVerify { inputStream -> IndexParser.parseEntry(inputStream) }
      } finally {
        entryFile.delete()
      }

    log.info { "Downloaded entry, now streaming index..." }

    val streamReceiver = RepoV2StreamReceiver(receiver, cert, repo.username, repo.password)
    val streamProcessor = IndexV2FullStreamProcessor(streamReceiver)
    val indexFile = tempFileProvider.createTempFile(entry.index.sha256)
    val inputStream =
      if (uri.scheme?.startsWith("http") == true) {
        // stream index for http(s) downloads
        val indexRequest =
          DownloadRequest(
            indexFile = entry.index,
            mirrors = repo.getMirrors(),
            proxy = proxy,
            username = repo.username,
            password = repo.password,
          )
        val digestInputStream = httpManager.getDigestInputStream(indexRequest)
        // wrap stream to exfiltrate index file for later usage
        SavingInputStream(digestInputStream, indexFile)
      } else {
        // no streaming supported, download file first
        val indexDownloader =
          downloaderFactory.create(
            repo = repo,
            uri = repoUriBuilder.getUri(repo, entry.index.name.trimStart('/')),
            indexFile = entry.index,
            destFile = indexFile,
          )
        indexDownloader.download()
        val digest = MessageDigest.getInstance("SHA-256")
        DigestInputStream(indexFile.inputStream(), digest)
      }
    inputStream.use { inputStream -> streamProcessor.process(entry.version, inputStream) {} }
    val hexDigest =
      when (inputStream) {
        is DigestInputStream -> inputStream.getDigestHex()
        is SavingInputStream -> inputStream.inputStream.getDigestHex()
        else -> error("Unknown InputStream ${inputStream::class.java}")
      }
    if (!hexDigest.equals(entry.index.sha256, ignoreCase = true)) {
      throw SigningException("Invalid ${entry.index.name} hash: $hexDigest")
    }
    return indexFile
  }
}
