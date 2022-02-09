# TimerManager
A lightweight and efficient global timer for various complex timing requirements of the entire app.

### for use

```TimerManager.addObserver(lo:LifecycleOwner, interval, name:String , androidx.lifcycle.Observer<Long>)```
  or
```TimerManager.addObserverForever(interval, name:String , androidx.lifcycle.Observer<Long>) ```
and 
```/**
     * add mutually exclusive observers. Only one observer of the same key is allowed to exist,
     * Always remove similar keys regardless of whether they are exactly the same as the newly injected observer
     * */
     addObserverUnique(lw: LifecycleOwner, interval: Long, key: String, observer: Observer<Long>)
 ```

  thats it, 
  
you can pause/resume timers with same of the timeline or by your observer key.

[removeObserver] is supported.

and you can call [release] when your app closing.
