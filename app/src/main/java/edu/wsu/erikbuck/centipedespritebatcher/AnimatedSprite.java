package edu.wsu.erikbuck.centipedespritebatcher;

import android.graphics.Rect;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by erik on 3/9/15.
 */


public class AnimatedSprite {
    private int mCellWidth;
    private int mCellHeight;
    private int mCellCount;
    private int mSpriteSheetID;
    private Map<String, Rect[]> mAnimations;

    private void init(String jsonString, int sheetID, int sheetWidth, int sheetHeight) {
        mSpriteSheetID = sheetID;
        mAnimations = new HashMap<String, Rect[]>();

        JSONParser parser=new JSONParser();
        try {
            JSONObject obj = (JSONObject)parser.parse(jsonString);

            JSONObject frames = (JSONObject)obj.get("frames");
            JSONObject animations;
            animations = (JSONObject)obj.get("animations");
            JSONArray  images = (JSONArray)obj.get("images");

            mCellWidth = (int) (long)frames.get("width");
            mCellHeight = (int) (long)frames.get("height");
            mCellCount = (int) (long)frames.get("count");

            int cellsPerRow = sheetWidth / mCellWidth;
            int rowsOnSheet = sheetHeight / mCellHeight;
            int cellsOnSheet = Math.min(rowsOnSheet * cellsPerRow, mCellCount);
            int w = mCellWidth;
            int h = mCellHeight;

            for ( Object key : animations.keySet()) {
                JSONArray value;
                value = (JSONArray)animations.get((String) key);

                int firstCellIndex = (int) (long)value.get(0);
                int lastCellIndex = (int) (long)value.get(1);
                int frameIndex = 0;

                Rect[] frameRects = new Rect[(lastCellIndex - firstCellIndex) + 1];
                mAnimations.put((String)key, frameRects);

                if(cellsOnSheet > lastCellIndex &&
                        1 <= sheetWidth ||
                        1 <= sheetHeight) {
                    for(int i = firstCellIndex; i <= lastCellIndex; i++)
                    {
                        int x = mCellWidth * (i % cellsPerRow);
                        int y = (mCellHeight * (i / cellsPerRow));

                        frameRects[frameIndex] = new Rect(x, y, x+w, y+h);
                        frameIndex++;
                    }
                }
            }

        } catch(ParseException pe) {
            System.out.println("position: " + pe.getPosition());
            System.out.println(pe);
        }
    }


    public AnimatedSprite(InputStream is, int sheetID, int sheetWidth, int sheetHeight) {


        Writer writer = new StringWriter();
        char[] buffer = new char[4096];
        try {
            try {
                try (Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
                    int n;
                    while ((n = reader.read(buffer)) != -1) {
                        writer.write(buffer, 0, n);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String jsonString = writer.toString();

        init(jsonString, sheetID, sheetWidth, sheetHeight);
    }


    public AnimatedSprite(String jsonString, int sheetID, int sheetWidth, int sheetHeight) {
        init(jsonString, sheetID, sheetWidth, sheetHeight);
    }

    public Rect[] frameRectsForAnimationName(String key) {
        return mAnimations.get(key);
    }
}
