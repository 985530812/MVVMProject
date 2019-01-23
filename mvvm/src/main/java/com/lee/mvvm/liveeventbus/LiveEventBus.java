package com.lee.mvvm.liveeventbus;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.Observer;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.lee.mvvm.liveeventbus.liveevent.LiveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by hailiangliao on 2018/7/4.
 */

public final class LiveEventBus {

    private final Map<String, BusLiveEvent<Object>> bus;

    private LiveEventBus() {
        bus = new HashMap<>();
    }

    private static class SingletonHolder {
        private static final LiveEventBus DEFAULT_BUS = new LiveEventBus();
    }

    public static LiveEventBus get() {
        return SingletonHolder.DEFAULT_BUS;
    }

    public synchronized <T> Observable<T> with(String key, Class<T> type) {
        return with(key, type, null);
    }

    public synchronized <T> Observable<T> with(String key, Class<T> type, Lifecycle.State activeState) {
        if (!bus.containsKey(key)) {
            BusLiveEvent event = new BusLiveEvent<>(key);
            if (activeState != null) {
                event.activeState = activeState;
            }
            bus.put(key, event);
        }
        return (Observable<T>) bus.get(key);
    }

    public Observable<Object> with(String key) {
        return with(key, Object.class);
    }

    public interface Observable<T> {
        void setValue(T value);

        void postValue(T value);

        void postValueDelay(T value, long delay, TimeUnit unit);

        void observe(@NonNull LifecycleOwner owner, @NonNull Observer<T> observer);

        void observeSticky(@NonNull LifecycleOwner owner, @NonNull Observer<T> observer);

        void observeForever(@NonNull Observer<T> observer);

        void observeStickyForever(@NonNull Observer<T> observer);

        void removeObserver(@NonNull Observer<T> observer);
    }

    private static class BusLiveEvent<T> extends LiveEvent<T> implements Observable<T> {

        private class PostValueTask implements Runnable {
            private Object newValue;

            public PostValueTask(@NonNull Object newValue) {
                this.newValue = newValue;
            }

            @Override
            public void run() {
                setValue((T) newValue);
            }
        }

        private Lifecycle.State activeState;

        @NonNull
        private final String key;
        private Handler mainHandler = new Handler(Looper.getMainLooper());

        private BusLiveEvent(String key) {
            this.key = key;
            this.activeState = super.observerActiveLevel();
        }

        @Override
        protected Lifecycle.State observerActiveLevel() {
            return activeState;
        }

        @Override
        public void postValueDelay(T value, long delay, TimeUnit unit) {
            mainHandler.postDelayed(new PostValueTask(value), unit.convert(delay, unit));
        }

        @Override
        public void removeObserver(@NonNull Observer<T> observer) {
            super.removeObserver(observer);
            if (!hasObservers()) {
                LiveEventBus.get().bus.remove(key);
            }
        }
    }
}
