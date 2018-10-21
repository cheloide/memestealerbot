package com.github.cheloide;

public class MemeStealerBot {

    public static void main(String[] args) {

        StealerThread runnable = new StealerThread();
        Thread        thread   = new Thread(runnable);
        thread.start();
    }
}
