package com.ebbyware.core.container;

public class Pair<T1, T2> {
    private T1 t1;
    private T2 t2;

    public Pair(T1 t1, T2 t2) {
        this.t1 = t1;
        this.t2 = t2;
    }

    public void setFirst(T1 t1) {
        this.t1 = t1;
    }

    public void setSecond(T2 t2) {
        this.t2 = t2;
    }

    public T1 first() {
        return t1;
    }

    public T2 second() {
        return t2;
    }

}
