package org.fdroid.download

/**
 * This is an interface for providing access to stored parameters for mirrors without adding
 * additional dependencies. The expectation is that this will be used to store and retrieve data
 * about mirror performance to use when ordering mirror for subsequent tests.
 *
 * Currently it supports success and error count, but other parameters could be added later.
 */
public interface MirrorParameterManager {

  /**
   * Set or get the number of failed attempts to access the specified mirror. The intent is to order
   * mirrors for subsequent tests based on the number of failures.
   */
  public fun incrementMirrorErrorCount(mirrorUrl: String)

  public fun getMirrorErrorCount(mirrorUrl: String): Int

  /**
   * Returns true or false depending on whether a particular mirror should be retried before moving
   * on to the next one (typically based on checking dns results)
   */
  public fun shouldRetryRequest(mirrorUrl: String): Boolean

  /**
   * Returns true or false depending on whether the location preference has been enabled. This
   * preference reflects whether mirrors matching your location should get priority.
   */
  public fun preferForeignMirrors(): Boolean

  /** Returns the country code of the user's current location */
  public fun getCurrentLocation(): String
}
