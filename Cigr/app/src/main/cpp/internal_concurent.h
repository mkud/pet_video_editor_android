/*
 * internal_concurent.h
 *
 *  Created on: 8 нояб. 2017 г.
 *      Author: maxx
 */

#ifndef INTERNAL_CONCURENT_H_
#define INTERNAL_CONCURENT_H_

#include <map>
#include <list>
#include <thread>
#include <mutex>
#include <condition_variable>

template<typename T>
class concurent_list {
public:

    T get() {
        std::unique_lock<std::mutex> mlock(mutex_);
        while (list_.empty() && !bFinish) {
            cond_.wait(mlock);
        }
        if (bFinish)
            return NULL;
        T item = list_.front();
        list_.pop_front();
        return item;
    }

    void put(const T &item) {
        std::unique_lock<std::mutex> mlock(mutex_);
        list_.push_back(item);
        mlock.unlock();
        cond_.notify_one();
    }

    void send_finish() {
        std::unique_lock<std::mutex> mlock(mutex_);
        bFinish = true;
        mlock.unlock();
        cond_.notify_all();
    }

    void put_init(const T &item) {
        list_.push_back(item);
    }

    unsigned int UnsyncGetCount() {
        return list_.size();
    }

private:
    std::list<T> list_;
    std::mutex mutex_;
    std::condition_variable cond_;

    bool bFinish = false;
};

template<typename K, typename T>
class concurent_map {
public:

    T getElement(K key) {
        std::unique_lock<std::mutex> mlock(mutex_);
        typename std::map<K, T>::iterator it;
        while ((it = map_.find(key)) == map_.end()) {
            cond_.wait(mlock);
        }
        T elem = it->second;
        map_.erase(it);
        return elem;
    }

    void put(const K &key, const T &item) {
        std::unique_lock<std::mutex> mlock(mutex_);
        map_[key] = item;
        mlock.unlock();
        cond_.notify_one();
    }

    unsigned int UnsyncGetCount() {
        return map_.size();
    }

private:
    std::map<K, T> map_;
    std::mutex mutex_;
    std::condition_variable cond_;
};

template<typename K, typename T>
class concurent_map2 {
public:

    T getElement() {
        std::unique_lock<std::mutex> mlock(mutex_);
        typename std::map<K, T>::iterator it;
        while (((it = map_.find(m_iCurKey)) == map_.end()) && !bFinish) {
            cond_.wait(mlock);
        }
        if (bFinish)
            return NULL;
        m_iCurKey++;
        T elem = it->second;
        map_.erase(it);
        return elem;
    }

    void put(const K &key, const T &item) {
        std::unique_lock<std::mutex> mlock(mutex_);
        map_[key] = item;
        mlock.unlock();
        cond_.notify_one();
    }

    void send_finish() {
        std::unique_lock<std::mutex> mlock(mutex_);
        bFinish = true;
        mlock.unlock();
        cond_.notify_all();
    }

    unsigned int UnsyncGetCount() {
        return map_.size();
    }

private:
    int m_iCurKey = 0;
    std::map<K, T> map_;
    std::mutex mutex_;
    std::condition_variable cond_;

    bool bFinish = false;
};

#endif /* INTERNAL_CONCURENT_H_ */
