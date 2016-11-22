package com.dean.mobileauto;
/**
 * Created by Zhy 2014/12/21.
 */
public class Judge {
    public  String translate(String cmd) {
        String f = "前";
        String b = "后";
        String l = "左";
        String r = "右";
        String data = "s";
        String dir;
        if(cmd.contains(f)){
            //System.out.println("f");
            dir="g";
        }else{
            if(cmd.contains(b)){

                //System.out.println("b");
                dir="n";
            }else{
                if(cmd.contains(l)){
                    // System.out.println("l");
                    dir="k90#";
                }else{
                    if(cmd.contains(r)){
                        //System.out.println("r");
                        dir="m90#";
                    }else{
                        //System.out.println("Wrong Cmd!");
                        dir="s";
                    }
                }
            }
        }
        int h=0;
        String dis="s";
        String[] str1 = new String[]{"二十","十九","十八","十七","十六","十五","十四","十三","十二","十一","十","九","八","七","六","五","四","三","二","一"};
        String[] str2 = new String[]{"20","19","18","17","16","15","14","13","12","11","10","9","8","7","6","5","4","3","2","1"};
        if(dir.contentEquals("s")) return dir;
        for(int i=0;i<20;i++)
        {

            if(cmd.contains(str1[i])){dis=str2[i]+"#";h=1;break;}
            if(cmd.contains(str2[i])){dis=str2[i]+"#";h=1;break;}
        }
        if(h==1) {
            if(dir.contentEquals("g") || dir.contentEquals("n")) {
                System.out.printf("%s", dir + dis);
                data = String.format("%s", dir + dis);
            }else{
                System.out.printf("%s", dir +"g"+ dis);
                data = String.format("%s", dir + "g" + dis);
            }
        }else { // 有方向没数字
            data = "s";
            if (dir.contentEquals("g")) {
                data = "f";
            } else if (dir.contentEquals("n")) {
                data = "b";
            } else if(dir.contentEquals("k90#")){
                data = "l";
            }else if(dir.contentEquals("m90#")){
                data = "r";
            }
        }
        return data;
    }
}