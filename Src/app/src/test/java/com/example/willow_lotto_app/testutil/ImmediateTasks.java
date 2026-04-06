package com.example.willow_lotto_app.testutil;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.mockito.stubbing.Answer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link com.google.android.gms.tasks.Tasks#forResult} uses Play Services executors that need
 * {@link android.os.Looper}, which plain JVM unit tests do not provide. These mocks run
 * {@link OnSuccessListener} on the calling thread instead.
 * <p>
 * Do not call {@code forResult} inside {@code when(...).thenReturn(...)} — Mockito treats that as
 * nested stubbing. Assign to a local variable first, then pass it to {@code thenReturn}.
 */
public final class ImmediateTasks {

    private ImmediateTasks() {
    }

    @SuppressWarnings("unchecked")
    public static <T> Task<T> forResult(T value) {
        Task<T> task = mock(Task.class);
        when(task.addOnSuccessListener(any(OnSuccessListener.class))).thenAnswer(
                (Answer<Task<T>>) invocation -> {
                    OnSuccessListener<T> listener = invocation.getArgument(0);
                    listener.onSuccess(value);
                    return task;
                });
        when(task.addOnFailureListener(any(OnFailureListener.class))).thenReturn(task);
        return task;
    }
}
