package org.spongycastle.pqc.jcajce.spec;

import java.security.spec.KeySpec;
import java.util.Vector;

import org.spongycastle.crypto.Digest;
import org.spongycastle.pqc.crypto.gmss.GMSSLeaf;
import org.spongycastle.pqc.crypto.gmss.GMSSParameters;
import org.spongycastle.pqc.crypto.gmss.GMSSRootCalc;
import org.spongycastle.pqc.crypto.gmss.GMSSRootSig;
import org.spongycastle.pqc.crypto.gmss.Treehash;
import org.spongycastle.util.Arrays;


/**
 * This class provides a specification for a GMSS private key.
 */
public class GMSSPrivateKeySpec
    implements KeySpec
{

    private int[] index;

    private byte[][] currentSeed;
    private byte[][] nextNextSeed;

    private byte[][][] currentAuthPath;
    private byte[][][] nextAuthPath;

    private Treehash[][] currentTreehash;
    private Treehash[][] nextTreehash;

    private Vector[] currentStack;
    private Vector[] nextStack;

    private Vector[][] currentRetain;
    private Vector[][] nextRetain;

    private byte[][][] keep;

    private GMSSLeaf[] nextNextLeaf;
    private GMSSLeaf[] upperLeaf;
    private GMSSLeaf[] upperTreehashLeaf;

    private int[] minTreehash;

    private GMSSParameters gmssPS;

    private byte[][] nextRoot;
    private GMSSRootCalc[] nextNextRoot;

    private byte[][] currentRootSig;
    private GMSSRootSig[] nextRootSig;

    /**
     * @param index             tree indices
     * @param currentSeed       seed for the generation of private OTS keys for the
     *                          current subtrees (TREE)
     * @param nextNextSeed      seed for the generation of private OTS keys for the
     *                          subtrees after next (TREE++)
     * @param currentAuthPath   array of current authentication paths (AUTHPATH)
     * @param nextAuthPath      array of next authentication paths (AUTHPATH+)
     * @param keep              keep array for the authPath algorithm
     * @param currentTreehash   treehash for authPath algorithm of current tree
     * @param nextTreehash      treehash for authPath algorithm of next tree (TREE+)
     * @param currentStack      shared stack for authPath algorithm of current tree
     * @param nextStack         shared stack for authPath algorithm of next tree (TREE+)
     * @param currentRetain     retain stack for authPath algorithm of current tree
     * @param nextRetain        retain stack for authPath algorithm of next tree (TREE+)
     * @param nextNextLeaf      array of upcoming leafs of the tree after next (LEAF++) of
     *                          each layer
     * @param upperLeaf         needed for precomputation of upper nodes
     * @param upperTreehashLeaf needed for precomputation of upper treehash nodes
     * @param minTreehash       index of next treehash instance to receive an update
     * @param nextRoot          the roots of the next trees (ROOT+)
     * @param nextNextRoot      the roots of the tree after next (ROOT++)
     * @param currentRootSig    array of signatures of the roots of the current subtrees
     *                          (SIG)
     * @param nextRootSig       array of signatures of the roots of the next subtree
     *                          (SIG+)
     * @param gmssParameterset  the GMSS Parameterset
     */
    public GMSSPrivateKeySpec(int[] index, byte[][] currentSeed,
                              byte[][] nextNextSeed, byte[][][] currentAuthPath,
                              byte[][][] nextAuthPath, Treehash[][] currentTreehash,
                              Treehash[][] nextTreehash, Vector[] currentStack,
                              Vector[] nextStack, Vector[][] currentRetain,
                              Vector[][] nextRetain, byte[][][] keep, GMSSLeaf[] nextNextLeaf,
                              GMSSLeaf[] upperLeaf, GMSSLeaf[] upperTreehashLeaf,
                              int[] minTreehash, byte[][] nextRoot, GMSSRootCalc[] nextNextRoot,
                              byte[][] currentRootSig, GMSSRootSig[] nextRootSig,
                              GMSSParameters gmssParameterset)
    {
        this.index = index;
        this.currentSeed = currentSeed;
        this.nextNextSeed = nextNextSeed;
        this.currentAuthPath = currentAuthPath;
        this.nextAuthPath = nextAuthPath;
        this.currentTreehash = currentTreehash;
        this.nextTreehash = nextTreehash;
        this.currentStack = currentStack;
        this.nextStack = nextStack;
        this.currentRetain = currentRetain;
        this.nextRetain = nextRetain;
        this.keep = keep;
        this.nextNextLeaf = nextNextLeaf;
        this.upperLeaf = upperLeaf;
        this.upperTreehashLeaf = upperTreehashLeaf;
        this.minTreehash = minTreehash;
        this.nextRoot = nextRoot;
        this.nextNextRoot = nextNextRoot;
        this.currentRootSig = currentRootSig;
        this.nextRootSig = nextRootSig;
        this.gmssPS = gmssParameterset;
    }

    public int[] getIndex()
    {
        return Arrays.clone(index);
    }

    public byte[][] getCurrentSeed()
    {
        return clone(currentSeed);
    }

    public byte[][] getNextNextSeed()
    {
        return clone(nextNextSeed);
    }

    public byte[][][] getCurrentAuthPath()
    {
        return clone(currentAuthPath);
    }

    public byte[][][] getNextAuthPath()
    {
        return clone(nextAuthPath);
    }

    public Treehash[][] getCurrentTreehash()
    {
        return clone(currentTreehash);
    }

    public Treehash[][] getNextTreehash()
    {
        return clone(nextTreehash);
    }

    public byte[][][] getKeep()
    {
        return clone(keep);
    }

    public Vector[] getCurrentStack()
    {
        return clone(currentStack);
    }

    public Vector[] getNextStack()
    {
        return clone(nextStack);
    }

    public Vector[][] getCurrentRetain()
    {
        return clone(currentRetain);
    }

    public Vector[][] getNextRetain()
    {
        return clone(nextRetain);
    }

    public GMSSLeaf[] getNextNextLeaf()
    {
        return clone(nextNextLeaf);
    }

    public GMSSLeaf[] getUpperLeaf()
    {
        return clone(upperLeaf);
    }

    public GMSSLeaf[] getUpperTreehashLeaf()
    {
        return clone(upperTreehashLeaf);
    }

    public int[] getMinTreehash()
    {
        return Arrays.clone(minTreehash);
    }

    public GMSSRootSig[] getNextRootSig()
    {
        return clone(nextRootSig);
    }

    public GMSSParameters getGmssPS()
    {
        return gmssPS;
    }

    public byte[][] getNextRoot()
    {
        return clone(nextRoot);
    }

    public GMSSRootCalc[] getNextNextRoot()
    {
        return clone(nextNextRoot);
    }

    public byte[][] getCurrentRootSig()
    {
        return clone(currentRootSig);
    }

    private static GMSSLeaf[] clone(GMSSLeaf[] data)
    {
        if (data == null)
        {
            return null;
        }
        GMSSLeaf[] copy = new GMSSLeaf[data.length];

        System.arraycopy(data, 0, copy, 0, data.length);

        return copy;
    }

    private static GMSSRootCalc[] clone(GMSSRootCalc[] data)
    {
        if (data == null)
        {
            return null;
        }
        GMSSRootCalc[] copy = new GMSSRootCalc[data.length];

        System.arraycopy(data, 0, copy, 0, data.length);

        return copy;
    }

    private static GMSSRootSig[] clone(GMSSRootSig[] data)
    {
        if (data == null)
        {
            return null;
        }
        GMSSRootSig[] copy = new GMSSRootSig[data.length];

        System.arraycopy(data, 0, copy, 0, data.length);

        return copy;
    }

    private static byte[][] clone(byte[][] data)
    {
        if (data == null)
        {
            return null;
        }
        byte[][] copy = new byte[data.length][];

        for (int i = 0; i != data.length; i++)
        {
            copy[i] = Arrays.clone(data[i]);
        }

        return copy;
    }

    private static byte[][][] clone(byte[][][] data)
    {
        if (data == null)
        {
            return null;
        }
        byte[][][] copy = new byte[data.length][][];

        for (int i = 0; i != data.length; i++)
        {
            copy[i] = clone(data[i]);
        }

        return copy;
    }

    private static Treehash[] clone(Treehash[] data)
    {
        if (data == null)
        {
            return null;
        }
        Treehash[] copy = new Treehash[data.length];

        System.arraycopy(data, 0, copy, 0, data.length);

        return copy;
    }

    private static Treehash[][] clone(Treehash[][] data)
    {
        if (data == null)
        {
            return null;
        }
        Treehash[][] copy = new Treehash[data.length][];

        for (int i = 0; i != data.length; i++)
        {
            copy[i] = clone(data[i]);
        }

        return copy;
    }

    private static Vector[] clone(Vector[] data)
    {
        if (data == null)
        {
            return null;
        }
        Vector[] copy = new Vector[data.length];

        for (int i = 0; i != data.length; i++)
        {
            copy[i] = new Vector(data[i]);
        }

        return copy;
    }

    private static Vector[][] clone(Vector[][] data)
    {
        if (data == null)
        {
            return null;
        }
        Vector[][] copy = new Vector[data.length][];

        for (int i = 0; i != data.length; i++)
        {
            copy[i] = clone(data[i]);
        }

        return copy;
    }
}