package edu.wsu.erikbuck.centipedespritebatcher;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import java.util.HashMap;

public class SoundManager {

    private SoundPool mSoundPool;
    private HashMap<Integer, Integer> mSoundPoolMap;
    private AudioManager  mAudioManager;
    private Context mContext;

    public SoundManager()
    {
    }

    public void initSounds(Context theContext) {
        mContext = theContext;
        mSoundPool = new SoundPool.Builder().build();
        mSoundPoolMap = new HashMap<Integer, Integer>();
        mAudioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
    }

    public void addSound(int index,int SoundID)
    {
        mSoundPoolMap.put(index, mSoundPool.load(mContext, SoundID, 1));
    }

    public void playSound(int index) {

        int streamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mSoundPool.play(mSoundPoolMap.get(index), streamVolume, streamVolume, 1, 0, 1f);
    }
}
