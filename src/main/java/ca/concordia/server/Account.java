package ca.concordia.server;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Account {
    //represent a bank account with a balance and withdraw and deposit methods
    private int balance;
    private final Lock lock = new ReentrantLock();
    private int id;

    public Account(int balance, int id){
        this.balance = balance;
        this.id = id;
    }

    public int getBalance(){
        return balance;
    }

    public void withdraw(int amount){
        lock.lock();
        try {
            balance -= amount;
        } finally {
            lock.unlock();
        }
    }

    public void deposit(int amount){
        lock.lock();
        try {
            this.balance += amount;
        } finally {
            lock.unlock();
        }
    }
}
