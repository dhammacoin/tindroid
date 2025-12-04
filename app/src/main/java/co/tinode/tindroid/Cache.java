package co.tinode.tindroid;

import android.annotation.SuppressLint;
import android.os.Build;
import android.telecom.CallAudioState;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

import com.google.firebase.messaging.FirebaseMessaging;

import co.tinode.tindroid.db.BaseDb;
import co.tinode.tindroid.media.VxCard;
import co.tinode.tindroid.services.CallConnection;
import co.tinode.tinodesdk.ComTopic;
import co.tinode.tinodesdk.FndTopic;
import co.tinode.tinodesdk.MeTopic;
import co.tinode.tinodesdk.PromisedReply;
import co.tinode.tinodesdk.Storage;
import co.tinode.tinodesdk.model.MsgServerData;
import co.tinode.tinodesdk.model.MsgServerInfo;
import co.tinode.tinodesdk.model.PrivateType;
import co.tinode.tinodesdk.model.ServerMessage;

public class Cache {
    private static final String TAG = "Cache";
    static final String API_KEY = "AQEAAAABAAD_rAp4DJh05a1HAwFT3A6K";

    private static final Cache sInstance = new Cache();

    private MyTinode mTinode = null;
    private String mTopicSelected = null;
    private CallInProgress mCallInProgress = null;

    @SuppressLint("UnsafeOptInUsageError")
    public static synchronized MyTinode getTinode() {
        if (sInstance.mTinode == null) {
            sInstance.mTinode = new MyTinode(
                    "Tindroid/" + TindroidApp.getAppVersion(),
                    API_KEY,
                    BaseDb.getInstance().getStore(),
                    null  // listener можно добавить отдельно через addListener()
            );

            sInstance.mTinode.setOsString(Build.VERSION.RELEASE);
            sInstance.mTinode.setDefaultTypeOfMetaPacket(VxCard.class, PrivateType.class);
            sInstance.mTinode.setMeTypeOfMetaPacket(VxCard.class);
            sInstance.mTinode.setFndTypeOfMetaPacket(VxCard.class);
            sInstance.mTinode.setLanguage(Locale.getDefault().toString());

            // Слушатель для звонков
            sInstance.mTinode.addListener(new MyTinode.EventListener() {
                @Override
                public void onDataMessage(MsgServerData data) {
                    if (Cache.getTinode().isMe(data.from)) return;

                    String webrtc = data.getStringHeader("webrtc");
                    MsgServerData.WebRTC callState = MsgServerData.parseWebRTC(webrtc);

                    ComTopic topic = (ComTopic) Cache.getTinode().getTopic(data.topic);
                    if (topic == null) return;

                    int effectiveSeq = UiUtils.parseSeqReference(data.getStringHeader("replace"));
                    if (effectiveSeq <= 0) effectiveSeq = data.seq;

                    Storage.Message msg = topic.getMessage(effectiveSeq);
                    if (msg != null) {
                        webrtc = msg.getStringHeader("webrtc");
                        if (webrtc != null &&
                                MsgServerData.parseWebRTC(webrtc) != callState) return;
                    }

                    switch (callState) {
                        case STARTED:
                            CallManager.acceptIncomingCall(
                                    TindroidApp.getAppContext(),
                                    data.topic,
                                    data.seq,
                                    data.getBooleanHeader(MyTinode.CALL_AUDIO_ONLY)
                            );
                            break;
                        case ACCEPTED:
                        case DECLINED:
                        case MISSED:
                        case DISCONNECTED:
                            CallInProgress call = Cache.getCallInProgress();
                            if (call != null && !call.isOutgoingCall()) {
                                CallManager.dismissIncomingCall(
                                        TindroidApp.getAppContext(),
                                        data.topic,
                                        data.seq
                                );
                            }
                            break;
                    }
                }

                @Override
                public void onInfoMessage(MsgServerInfo info) {
                    if (MsgServerInfo.parseWhat(info.what) != MsgServerInfo.What.CALL) return;

                    CallInProgress call = Cache.getCallInProgress();
                    if (call == null ||
                            !call.equals(info.src, info.seq) ||
                            !MyTinode.TOPIC_ME.equals(info.topic)) return;

                    if (MsgServerInfo.parseEvent(info.event) == MsgServerInfo.Event.HANG_UP ||
                            (Cache.getTinode().isMe(info.from) &&
                                    MsgServerInfo.parseEvent(info.event) == MsgServerInfo.Event.ACCEPT)) {
                        CallManager.dismissIncomingCall(
                                TindroidApp.getAppContext(),
                                info.src,
                                info.seq
                        );
                    }
                }
            });

            TindroidApp.retainCache(sInstance);
        }

        return sInstance.mTinode;
    }

    static void invalidate() {
        endCallInProgress();
        setSelectedTopicName(null);

        if (sInstance.mTinode != null) {
            sInstance.mTinode.logout();
            sInstance.mTinode = null;
        }

        SharedPrefs.deleteToken(TindroidApp.getAppContext());
        FirebaseMessaging.getInstance().deleteToken();
    }

    public static void reconnect() {
        MyTinode tinode = getTinode();

        if (!tinode.isConnected()) {
            Log.w(TAG, "Not connected yet");
            return;
        }

        String token = SharedPrefs.getToken(TindroidApp.getAppContext());

        if (tinode.isAuthenticated()) return;

        if (token != null) {
            Log.d(TAG, "Trying token login...");
            tinode.loginWithToken(token);
        } else {
            Log.d(TAG, "Creating anonymous account...");
            tinode.loginAnon();
        }
    }

    // ======= Остальной стандартный код =======
    public static CallInProgress getCallInProgress() { return sInstance.mCallInProgress; }
    public static void prepareNewCall(@NonNull String topic, int seq, @Nullable CallConnection conn) {
        if (sInstance.mCallInProgress == null) {
            sInstance.mCallInProgress = new CallInProgress(topic, seq, conn);
        }
    }
    public static void setCallActive(String topic, int seqId) { if (sInstance.mCallInProgress != null) sInstance.mCallInProgress.setCallActive(topic, seqId); }
    public static void setCallConnected() { if (sInstance.mCallInProgress != null) sInstance.mCallInProgress.setCallConnected(); }
    public static void endCallInProgress() { if (sInstance.mCallInProgress != null) { sInstance.mCallInProgress.endCall(); sInstance.mCallInProgress = null; } }
    public static boolean setCallAudioRoute(int route) { return sInstance.mCallInProgress != null && sInstance.mCallInProgress.setAudioRoute(route); }
    public static int getCallAudioRoute() { return sInstance.mCallInProgress != null ? sInstance.mCallInProgress.getAudioRoute() : CallAudioState.ROUTE_EARPIECE; }
    public static boolean isCallUseful() { return sInstance.mCallInProgress != null && sInstance.mCallInProgress.isConnectionUseful(); }
    public static String getSelectedTopicName() { return sInstance.mTopicSelected; }
    public static void setSelectedTopicName(String topicName) {
        String old = sInstance.mTopicSelected;
        sInstance.mTopicSelected = topicName;
        if (sInstance.mTinode != null && old != null && !old.equals(topicName)) {
            ComTopic topic = (ComTopic) sInstance.mTinode.getTopic(old);
            if (topic != null) topic.leave();
        }
    }
    @SuppressWarnings("unchecked")
    public static PromisedReply<ServerMessage> attachMeTopic(MeTopic.MeListener l) {
        final MeTopic<VxCard> me = getTinode().getOrCreateMeTopic();
        if (l != null) me.setListener(l);
        if (!me.isAttached()) return me.subscribe(null, me.getMetaGetBuilder().withCred().withDesc().withSub().withTags().build());
        return new PromisedReply<>((ServerMessage) null);
    }
    static PromisedReply<ServerMessage> attachFndTopic(FndTopic.FndListener<VxCard> l) {
        final FndTopic<VxCard> fnd = getTinode().getOrCreateFndTopic();
        if (l != null) fnd.setListener(l);
        if (!fnd.isAttached()) return fnd.subscribe(null, null);
        return new PromisedReply<>((ServerMessage) null);
    }
}
