package ca.concordia.server;

import java.util.Comparator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Account {
    //represent a bank account with a balance and withdraw and deposit methods
    private int balance;
    private final Lock lock;
    private int id;

    // Comparator for establishing lock ordering based on account IDs
    private static final Comparator<Account> accountComparator = Comparator.comparingInt(account -> account.id);

    public Account(int balance, int id){
        this.balance = balance;
        this.id = id;
        lock = new ReentrantLock();
    }

    public int getBalance(){
        return balance;
    }

    public void deposit(int amount) {
        balance += amount;
    }

    public void withdraw(int amount) {
        balance -= amount;
    }

    public void transferFunds(Account destination, int amount) {
        // Acquire locks based on the order of account IDs
        Account first = accountComparator.compare(this, destination) <= 0 ? this : destination;
        Account second = first == this ? destination : this;

        first.lock.lock();
        try {
            second.lock.lock();
            try {
                if (amount >= 0 && balance >= amount) {
                    withdraw(amount);
                    destination.deposit(amount);
                }
            } finally {
                second.lock.unlock();
            }
        } finally {
            first.lock.unlock();
        }
    }
}
