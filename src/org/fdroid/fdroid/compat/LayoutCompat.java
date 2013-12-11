package org.fdroid.fdroid.compat;

public abstract class LayoutCompat extends Compatibility {

    public static LayoutCompat create() {
        if (hasApi(17)) {
            return new JellyBeanMr1LayoutCompatImpl();
        } else {
            return new OldLayoutCompatImpl();
        }
    }

    private static final LayoutCompat impl = LayoutCompat.create();

    protected abstract int relativeLayoutEndOf();

    public static class RelativeLayout {
        public static final int END_OF = impl.relativeLayoutEndOf();
    }

}

class OldLayoutCompatImpl extends LayoutCompat {

    @Override
    protected int relativeLayoutEndOf() {
        return android.widget.RelativeLayout.RIGHT_OF;
    }
}

class JellyBeanMr1LayoutCompatImpl extends LayoutCompat {

    @Override
    protected int relativeLayoutEndOf() {
        return android.widget.RelativeLayout.END_OF;
    }
}
