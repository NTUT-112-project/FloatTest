# fix records

## Attempt to invoke virtual method 'android.content.res.Resources android.content.Context.getResources()' on a null object reference

07:47:17.647  I  START u0 {cmp=com.example.floattest/.menuActivity (has extras)} from uid 10086
07:47:17.658  W  handleWindowVisibility: no activity for token android.os.BinderProxy@ad031a0
--------- beginning of crash
07:47:17.664  D  Shutting down VM
07:47:17.668  E  FATAL EXCEPTION: main (Ask Gemini)
                 Process: com.example.floattest, PID: 4238
                 java.lang.RuntimeException: Unable to instantiate activity ComponentInfo{com.example.floattest/com.example.floattest.menuActivity}: java.lang.NullPointerException: Attempt to invoke virtual method 'android.content.res.Resources android.content.Context.getResources()' on a null object reference
                  at android.app.ActivityThread.performLaunchActivity(ActivityThread.java:2843)
                  at android.app.ActivityThread.handleLaunchActivity(ActivityThread.java:3048)
                  at android.app.servertransaction.LaunchActivityItem.execute(LaunchActivityItem.java:78)
                  at android.app.servertransaction.TransactionExecutor.executeCallbacks(TransactionExecutor.java:108)
                  at android.app.servertransaction.TransactionExecutor.execute(TransactionExecutor.java:68)
                  at android.app.ActivityThread$H.handleMessage(ActivityThread.java:1808)
                  at android.os.Handler.dispatchMessage(Handler.java:106)
                  at android.os.Looper.loop(Looper.java:193)
                  at android.app.ActivityThread.main(ActivityThread.java:6669)
                  at java.lang.reflect.Method.invoke(Native Method)
                  at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:493)
                  at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:858)
                 Caused by: java.lang.NullPointerException: Attempt to invoke virtual method 'android.content.res.Resources android.content.Context.getResources()' on a null object reference
                  at android.content.ContextWrapper.getResources(ContextWrapper.java:91)
                  at android.view.ContextThemeWrapper.getResourcesInternal(ContextThemeWrapper.java:127)
                  at android.view.ContextThemeWrapper.getResources(ContextThemeWrapper.java:121)
                  at androidx.appcompat.app.AppCompatActivity.getResources(AppCompatActivity.java:612)
                  at com.example.floattest.menuActivity.<init>(menuActivity.kt:58)
                  at java.lang.Class.newInstance(Native Method)
                  at android.app.AppComponentFactory.instantiateActivity(AppComponentFactory.java:69)
                  at androidx.core.app.CoreComponentFactory.instantiateActivity(CoreComponentFactory.java:44)
                  at android.app.Instrumentation.newActivity(Instrumentation.java:1215)
                  at android.app.ActivityThread.performLaunchActivity(ActivityThread.java:2831)
                  at android.app.ActivityThread.handleLaunchActivity(ActivityThread.java:3048)
                  at android.app.servertransaction.LaunchActivityItem.execute(LaunchActivityItem.java:78)
                  at android.app.servertransaction.TransactionExecutor.executeCallbacks(TransactionExecutor.java:108)
                  at android.app.servertransaction.TransactionExecutor.execute(TransactionExecutor.java:68)
                  at android.app.ActivityThread$H.handleMessage(ActivityThread.java:1808)
                  at android.os.Handler.dispatchMessage(Handler.java:106)
                  at android.os.Looper.loop(Looper.java:193)
                  at android.app.ActivityThread.main(ActivityThread.java:6669)
                  at java.lang.reflect.Method.invoke(Native Method)
                  at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:493)
                  at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:858)
