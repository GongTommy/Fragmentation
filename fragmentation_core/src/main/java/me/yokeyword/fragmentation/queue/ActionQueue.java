package me.yokeyword.fragmentation.queue;

import android.os.Handler;

import java.util.LinkedList;
import java.util.Queue;

import me.yokeyword.fragmentation.ISupportFragment;
import me.yokeyword.fragmentation.SupportHelper;

/**
 * The queue of perform action.
 * <p>
 * Created by YoKey on 17/12/29.
 */
public class ActionQueue {
    private Queue<Action> mQueue = new LinkedList<>();
    private Handler mMainHandler;

    public ActionQueue(Handler mainHandler) {
        this.mMainHandler = mainHandler;
    }

    public void enqueue(final Action action) {
        if (isThrottleBACK(action)) return;

        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                enqueueAction(action);
            }
        });
    }

    private void enqueueAction(Action action) {
        mQueue.add(action);
        if (mQueue.size() == 1) {
            handleAction();
        }
    }

    private void handleAction() {
        if (mQueue.isEmpty()) return;

        Action action = mQueue.peek();
        action.run();

        handleNextAction(action);
    }

    private void handleNextAction(final Action action) {
        if (action.action == Action.ACTION_LOAD) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    executeNextAction(action);
                }
            });
        } else {
            executeNextAction(action);
        }
    }

    private void executeNextAction(Action action) {
        if (action.action == Action.ACTION_LOAD || action.action == Action.ACTION_POP) {
            ISupportFragment top = SupportHelper.getTopFragment(action.fragmentManager);
            if (top == null) return;
            long duration = action.action == Action.ACTION_LOAD ?
                    top.getSupportDelegate().getEnterAnimDuration() : top.getSupportDelegate().getExitAnimDuration();
            action.duration = duration + Action.BUFFER_TIME;
        }

        mMainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mQueue.poll();
                handleAction();
            }
        }, action.duration);
    }

    private boolean isThrottleBACK(Action action) {
        if (action.action == Action.ACTION_BACK) {
            Action head = mQueue.peek();
            if (head != null && head.action == Action.ACTION_POP) {
                return true;
            }
        }
        return false;
    }
}
