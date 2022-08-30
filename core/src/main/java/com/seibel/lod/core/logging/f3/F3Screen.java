package com.seibel.lod.core.logging.f3;

import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.a7.datatype.column.accessor.ColumnArrayView;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

public class F3Screen {
    public static boolean renderCustomF3 = true;

    private static final String[] DEFAULT_STR = {
            "",
            ModInfo.READABLE_NAME + " version: " + ModInfo.VERSION
    };
    private static final LinkedList<WeakReference<SelfUpdateMessage>> selfUpdateMessages = new LinkedList<>();
    public static void addStringToDisplay(List<String> list) {
        list.addAll(Arrays.asList(DEFAULT_STR));
        Iterator<WeakReference<SelfUpdateMessage>> it = selfUpdateMessages.iterator();
        while (it.hasNext()) {
            WeakReference<SelfUpdateMessage> ref = it.next();
            SelfUpdateMessage msg = ref.get();
            if (msg == null) {
                it.remove();
            } else {
                msg.print(list);
            }
        }
    }

    public static class SelfUpdateMessage {
        private final Supplier<String> supplier;
        public SelfUpdateMessage(Supplier<String> message) {
            selfUpdateMessages.add(new WeakReference<>(this));
            this.supplier = message;
        }
        public void print(List<String> list) {
            list.add(supplier.get());
        }
    }
}
