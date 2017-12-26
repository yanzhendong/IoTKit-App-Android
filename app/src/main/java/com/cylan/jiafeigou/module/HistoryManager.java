package com.cylan.jiafeigou.module;

import com.cylan.entity.jniCall.JFGHistoryVideo;
import com.cylan.entity.jniCall.JFGVideo;
import com.cylan.ex.JfgException;
import com.cylan.jiafeigou.support.log.AppLogger;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by yanzhendong on 2017/12/25.
 */

public class HistoryManager {
    private static HistoryManager instance;
    private ConcurrentHashMap<String, HistoryObserver> historyObserverHashMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, TreeSet<JFGVideo>> historyVideoMap = new ConcurrentHashMap<>();
    private HashSet<String> notifyQueen = new HashSet<>();

    private Comparator<JFGVideo> comparator = new Comparator<JFGVideo>() {
        @Override
        public int compare(JFGVideo o1, JFGVideo o2) {
            return (int) (o1.beginTime - o2.beginTime);
        }
    };

    public static HistoryManager getInstance() {
        if (instance == null) {
            synchronized (HistoryManager.class) {
                if (instance == null) {
                    instance = new HistoryManager();
                }
            }
        }
        return instance;
    }


    public interface HistoryObserver {
        void onHistoryChanged(Collection<JFGVideo> history);
    }

    public void addHistoryObserver(String uuid, HistoryObserver observer) {
        historyObserverHashMap.put(uuid, observer);
        TreeSet<JFGVideo> jfgVideos = historyVideoMap.get(uuid);
        if (jfgVideos != null && jfgVideos.size() > 0) {
            observer.onHistoryChanged(getHistory(uuid));
        }
    }

    public void removeHistoryObserver(String uuid) {
        historyObserverHashMap.remove(uuid);
    }

    public boolean hasHistory(String uuid) {
        TreeSet<JFGVideo> jfgVideos = historyVideoMap.get(uuid);
        return jfgVideos != null && jfgVideos.size() > 0;
    }

    public TreeSet<JFGVideo> getHistory(String uuid) {
        return historyVideoMap.get(uuid);
    }

    public JFGVideo getMinHistory(String uuid) {
        TreeSet<JFGVideo> jfgVideos = historyVideoMap.get(uuid);
        return jfgVideos != null ? jfgVideos.first() : null;
    }

    public void cacheHistory(JFGHistoryVideo historyVideo) {
        if (historyVideo.list != null && historyVideo.list.size() > 0) {
            synchronized (this) {
                notifyQueen.clear();
                for (JFGVideo video : historyVideo.list) {
                    TreeSet<JFGVideo> jfgVideos = historyVideoMap.get(video.peer);
                    if (jfgVideos == null) {
                        jfgVideos = new TreeSet<>(comparator);
                        historyVideoMap.put(video.peer, jfgVideos);
                    }
                    jfgVideos.add(video);
                    notifyQueen.add(video.peer);
                }

                for (String peer : notifyQueen) {
                    HistoryObserver historyObserver = historyObserverHashMap.get(peer);
                    if (historyObserver != null) {
                        historyObserver.onHistoryChanged(getHistory(peer));
                    }
                }

            }
        }
    }

    public void cacheHistory(byte[] bytes) {
        HistoryV2Manager.getInstance().cacheHistory(bytes);
    }

    public void fetchHistoryV1(String uuid) {
        try {
            Command.getInstance().getVideoList(uuid);
//            Command.getInstance().getVideoListV2(uuid, (int) (System.currentTimeMillis() / 1000), 1, 365);
        } catch (JfgException e) {
            e.printStackTrace();
            AppLogger.e(e);
        }
    }

    public void fetchHistoryV2(String uuid, int beginTime, int way, int num) {
        fetchHistoryV1(uuid);
//        HistoryV2Manager.getInstance().fetchHistoryV2(uuid, beginTime, way, num);
    }

    private static class HistoryV2Manager {

        private static class PendingQueryAction {
            public String uuid;
            public int beginTime;
            public int way;
            public int num;
        }

        private static HistoryV2Manager instance;
        private TreeSet<PendingQueryAction> pendingQueryActions = new TreeSet<>();

        public static HistoryV2Manager getInstance() {
            if (instance == null) {
                synchronized (HistoryV2Manager.class) {
                    if (instance == null) {
                        instance = new HistoryV2Manager();
                    }
                }
            }
            return instance;
        }

        void fetchHistoryV2(String uuid, int beginTime, int way, int num) {
            try {
                Command.getInstance().getVideoListV2(uuid, beginTime, way, num);
//                pendingQueryActions.
            } catch (JfgException e) {
                e.printStackTrace();
                AppLogger.e(e);
            }
        }


        void cacheHistory(byte[] bytes) {

        }

    }

}
