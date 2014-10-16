package org.fdroid.fdroid.compat;

import java.io.File;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Environment;

public abstract class ContextCompat extends Compatibility {

    public static ContextCompat create(Context context) {
        if (hasApi(8)) {
            return new FroyoContextCompatImpl(context);
        } else {
            return new OldContextCompatImpl(context);
        }
    }

    protected final Context context;

    public ContextCompat(Context context) {
        this.context = context;
    }

    /**
     * @see android.content.Context#getExternalCacheDir()
     */
    public abstract File getExternalCacheDir();

}

class OldContextCompatImpl extends ContextCompat {

    public OldContextCompatImpl(Context context) {
        super(context);
    }

    @Override
    public File getExternalCacheDir() {
        File file = new File(Environment.getExternalStorageDirectory(),
                "Android/data/org.fdroid.fdroid/cache");
        if (!file.exists())
            file.mkdirs();
        return file;
    }

}

@TargetApi(8)
class FroyoContextCompatImpl extends ContextCompat {

    public FroyoContextCompatImpl(Context context) {
        super(context);
    }

    @Override
    public File getExternalCacheDir() {
        return context.getExternalCacheDir();
    }

}
