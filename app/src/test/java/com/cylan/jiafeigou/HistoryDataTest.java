package com.cylan.jiafeigou;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by cylan-hunt on 16-9-30.
 */
@RunWith(MyTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class HistoryDataTest {
    @Test
    public void testDataAssemblePerformance() {
        String testData = "[[1475077154,30],[1475077191,30],[1475077274,30],[1475077346,30],[1475077451,30],[1475077483,30],[1475077552,30],[1475077602,30],[1475077658,30],[1475077691,30],[1475077726,30],[1475077758,30],[1475077789,30],[1475077864,30],[1475077912,30],[1475077947,30],[1475077979,30],[1475078011,30],[1475078043,30],[1475078078,30],[1475078115,30],[1475078146,30],[1475078179,30],[1475078215,30],[1475078253,30],[1475078304,30],[1475078336,30],[1475078368,30],[1475078399,0],[1475078400,29],[1475078431,30],[1475078467,30],[1475078506,30],[1475078540,30],[1475078572,30],[1475078605,30],[1475078640,30],[1475078682,30],[1475078714,30],[1475078810,30],[1475078899,30],[1475078971,30],[1475079002,30],[1475079046,30],[1475079095,30],[1475079127,30],[1475079161,30],[1475079193,30],[1475079225,30],[1475079276,30],[1475079336,30],[1475079414,27],[1475079475,30],[1475079553,30],[1475079617,26],[1475079700,30],[1475079739,30],[1475079771,30],[1475079809,30],[1475079841,30],[1475079877,30],[1475079914,30],[1475079948,30],[1475079987,30],[1475080018,30],[1475080050,30],[1475080086,30],[1475080118,30],[1475080153,30],[1475080196,30],[1475080252,30],[1475080284,30],[1475080315,30],[1475080347,30],[1475080395,30],[1475080436,30],[1475080474,30],[1475080516,30],[1475080550,30],[1475080590,30],[1475080629,30],[1475080662,30],[1475080701,30],[1475080733,30],[1475080766,30],[1475080798,30],[1475080833,30],[1475080866,30],[1475080898,30],[1475080938,30],[1475080970,30],[1475081003,30],[1475081034,30],[1475081069,30],[1475081101,30],[1475081147,30],[1475081181,30],[1475081213,30],[1475081245,30],[1475081282,30],[1475081318,30],[1475081349,30],[1475081393,30],[1475081427,30],[1475081459,30],[1475081494,30],[1475081526,30],[1475081558,30],[1475081590,30],[1475081622,30],[1475081654,30],[1475081686,30],[1475081733,30],[1475081776,30],[1475081807,30],[1475081839,30],[1475081872,30],[1475081905,30],[1475081937,30],[1475081970,29],[1475082000,0],[1475082003,30],[1475082040,30],[1475082072,30],[1475082104,30],[1475082136,30],[1475082201,30],[1475082286,30],[1475082322,30],[1475082354,30],[1475082386,30],[1475082431,30],[1475082476,30],[1475082534,30],[1475082570,30],[1475082614,30],[1475082681,30],[1475082755,30],[1475082876,30],[1475082927,30],[1475083007,30],[1475083102,30],[1475083230,27],[1475083310,30],[1475083343,30],[1475083388,30],[1475083436,30],[1475083469,30],[1475083504,30],[1475083564,30],[1475083676,30],[1475083738,30],[1475083771,30],[1475083803,30],[1475083863,30],[1475083911,30],[1475083969,30],[1475084021,30],[1475084062,30],[1475084101,30],[1475084133,30],[1475084170,30],[1475084202,30],[1475084249,30],[1475084301,30],[1475084355,30],[1475084391,30],[1475084423,30],[1475084457,30],[1475084491,30],[1475084534,30],[1475084576,30],[1475084633,30],[1475084687,30],[1475084720,30],[1475084828,30],[1475084920,30],[1475084968,30],[1475085043,30],[1475085104,8],[1475089016,0]]]";
        String[] tmpArray = testData.split(",");
        System.out.println(tmpArray.length);
        ArrayList<Long> list = new ArrayList<>();
        for (int i = 0; i < tmpArray.length; i++) {
            if (tmpArray[i].contains("14750"))
                list.add(1000L * Long.parseLong(tmpArray[i].replace("[", "").replace("]", "")));
        }
        System.out.println(list.size());
        System.out.println(list);
        SimpleDateFormat format = new SimpleDateFormat("", Locale.getDefault());
    }
}
