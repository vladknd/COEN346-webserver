package ca.concordia.server;
import java.util.Comparator;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Account {
    //represent a bank account with a balance and withdraw and deposit methods
    private int balance;
    private final Lock lock;
    private Semaphore semaphore = new Semaphore(1);
    private int id;

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
        try {
            // Acquire semaphores in a consistent order based on account IDs to avoid deadlocks
            Account first = this.id < destination.id ? this : destination;
            Account second = first == this ? destination : this;

            first.semaphore.acquire();
            try {
                second.semaphore.acquire();
                try {
                    if (amount >= 0 && this.balance >= amount) {
                        this.withdraw(amount);
                        destination.deposit(amount);
                    }
                } finally {
                    second.semaphore.release();
                }
            } finally {
                first.semaphore.release();
            }
        } catch (InterruptedException e) {
            // Handle interrupted exception
            Thread.currentThread().interrupt();
        }
    }
}
