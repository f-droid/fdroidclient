package org.fdroid.fdroid.compat;

import android.annotation.TargetApi;

public abstract class LayoutCompat extends Compatibility {

    public static LayoutCompat create() {
        if (hasApi(17)) {
            return new JellyBeanMr1LayoutCompatImpl();
        } else {
            return new OldLayoutCompatImpl();
        }
    }

    private static final LayoutCompat impl = LayoutCompat.create();

    protected abstract int relativeLayoutStartOf();
    protected abstract int relativeLayoutEndOf();
    protected abstract int relativeLayoutAlignParentStart();
    protected abstract int relativeLayoutAlignParentEnd();

    public static class RelativeLayout {
        public static final int START_OF = impl.relativeLayoutStartOf();
        public static final int END_OF = impl.relativeLayoutEndOf();
        public static final int ALIGN_PARENT_START = impl.relativeLayoutAlignParentStart();
        public static final int ALIGN_PARENT_END = impl.relativeLayoutAlignParentEnd();
    }

}

class OldLayoutCompatImpl extends LayoutCompat {

    @Override
    protected int relativeLayoutStartOf() {
        return android.widget.RelativeLayout.LEFT_OF;
    }

    @Override
    protected int relativeLayoutEndOf() {
        return android.widget.RelativeLayout.RIGHT_OF;
    }

    @Override
    protected int relativeLayoutAlignParentStart() {
        return android.widget.RelativeLayout.ALIGN_PARENT_LEFT;
    }

    @Override
    protected int relativeLayoutAlignParentEnd() {
        return android.widget.RelativeLayout.ALIGN_PARENT_RIGHT;
    }
}

@TargetApi(17)
class JellyBeanMr1LayoutCompatImpl extends LayoutCompat {

    @Override
    protected int relativeLayoutStartOf() {
        return android.widget.RelativeLayout.START_OF;
    }

    @Override
    protected int relativeLayoutEndOf() {
        return android.widget.RelativeLayout.END_OF;
    }

    @Override
    protected int relativeLayoutAlignParentStart() {
        return android.widget.RelativeLayout.ALIGN_PARENT_START;
    }

    @Override
    protected int relativeLayoutAlignParentEnd() {
        return android.widget.RelativeLayout.ALIGN_PARENT_END;
    }
}
