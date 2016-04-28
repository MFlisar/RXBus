package com.michaelflisar.rxbus.rx;

import java.util.Queue;
import java.util.concurrent.atomic.*;

import rx.*;
import rx.Observable.Operator;
import rx.exceptions.MissingBackpressureException;
import rx.internal.operators.*;
import rx.internal.util.*;
import rx.internal.util.atomic.SpscAtomicArrayQueue;

public final class RxValve<T> implements Operator<T, T> {

    final Observable<Boolean> other;

    final int prefetch;

    final boolean defaultState;

    public RxValve(Observable<Boolean> other, int prefetch, boolean defaultState) {
        this.other = other;
        this.prefetch = prefetch;
        this.defaultState = defaultState;
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super T> child) {
        ValveSubscriber<T> parent = new ValveSubscriber<>(child, prefetch, defaultState);

        OtherSubscriber os = new OtherSubscriber(parent);
        parent.other = os;

        child.add(parent);
        child.add(os);
        child.setProducer(r -> parent.requestInner(r));

        other.subscribe(os);

        return parent;
    }

    static final class ValveSubscriber<T> extends Subscriber<T> {
        final Subscriber<? super T> actual;

        final int limit;

        final AtomicLong requested;

        final AtomicInteger wip;

        final AtomicReference<Throwable> error;

        final Queue<Object> queue;

        final NotificationLite<T> nl;

        volatile boolean valveOpen;

        volatile boolean done;

        Subscription other;

        long emission;

        public ValveSubscriber(Subscriber<? super T> actual, int prefetch, boolean defaultState) {
            this.actual = actual;
            this.limit = prefetch - (prefetch >> 2);
            this.requested = new AtomicLong();
            this.wip = new AtomicInteger();
            this.error = new AtomicReference<>();
            this.queue = new SpscAtomicArrayQueue<>(prefetch);
            this.nl = NotificationLite.instance();
            request(prefetch);
            this.valveOpen = defaultState;
        }

        @Override
        public void onNext(T t) {
            if (!queue.offer(nl.next(t))) {
                onError(new MissingBackpressureException());
            } else {
                drain();
            }
        }

        @Override
        public void onError(Throwable e) {
            if (ExceptionsUtils.addThrowable(error, e)) {
                other.unsubscribe();
                done = true;
                drain();
            } else {
                RxJavaPluginUtils.handleException(e);
            }
        }

        @Override
        public void onCompleted() {
            done = true;
            drain();
        }

        void otherSignal(boolean state) {
            valveOpen = state;
            if (state) {
                drain();
            }
        }

        void otherError(Throwable e) {
            if (ExceptionsUtils.addThrowable(error, e)) {
                unsubscribe();
                done = true;
                drain();
            } else {
                RxJavaPluginUtils.handleException(e);
            }
        }

        void otherCompleted() {
            if (ExceptionsUtils.addThrowable(error, new IllegalStateException("Other completed unexpectedly"))) {
                unsubscribe();
                done = true;
                drain();
            }
        }

        void requestInner(long n) {
            if (n > 0) {
                BackpressureUtils.getAndAddRequest(requested, n);
                drain();
            } else
            if (n < 0) {
                throw new IllegalArgumentException("n >= 0 required but it was " + n);
            }
        }

        void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }

            int missed = 1;

            for (;;) {

                if (valveOpen) {
                    long r = requested.get();
                    long e = emission;

                    while (e != r && valveOpen) {
                        if (actual.isUnsubscribed()) {
                            return;
                        }

                        if (error.get() != null) {
                            unsubscribe();
                            other.unsubscribe();
                            queue.clear();
                            Throwable ex = ExceptionsUtils.terminate(error);
                            actual.onError(ex);
                            return;
                        }

                        boolean d = done;
                        Object o = queue.poll();
                        boolean empty = o == null;

                        if (d && empty) {
                            other.unsubscribe();
                            actual.onCompleted();
                            return;
                        }

                        if (empty) {
                            break;
                        }

                        e++;
                        if (e == limit) {
                            r = BackpressureUtils.produced(requested, e);
                            request(e);
                            e = 0L;
                        }
                    }

                    if (e == r) {
                        if (actual.isUnsubscribed()) {
                            return;
                        }

                        if (error.get() != null) {
                            unsubscribe();
                            other.unsubscribe();
                            queue.clear();
                            Throwable ex = ExceptionsUtils.terminate(error);
                            actual.onError(ex);
                            return;
                        }

                        if (done && queue.isEmpty()) {
                            other.unsubscribe();
                            actual.onCompleted();
                            return;
                        }
                    }

                    emission = e;
                } else {
                    if (actual.isUnsubscribed()) {
                        return;
                    }
                }

                missed = wip.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
    }

    static final class OtherSubscriber extends Subscriber<Boolean> {
        final ValveSubscriber<?> valve;

        public OtherSubscriber(ValveSubscriber<?> valve) {
            this.valve = valve;
        }

        @Override
        public void onNext(Boolean t) {
            valve.otherSignal(t);
        }

        @Override
        public void onError(Throwable e) {
            valve.otherError(e);
        }

        @Override
        public void onCompleted() {
            valve.otherCompleted();
        }
    }
}

