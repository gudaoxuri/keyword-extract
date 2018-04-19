package com.ecfront.kwe.test;

import com.ecfront.kwe.KeyWordExtract;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class KeyWordExtractTest {

    @Test
    public void testAll() {
        String sKey = "URL关键词提取";
        String[] urls = new String[]{
                // 通用
                "https://www.baidu.com/s?ie=utf-8&f=8&rsv_bp=1&rsv_idx=1&tn=baidu&wd=URL%E5%85%B3%E9%94%AE%E8%AF%8D%E6%8F%90%E5%8F%96&oq=%25E6%2590%259C%25E7%25B4%25A2%25E8%25AF%258D%2520%25E8%25A7%25A3%25E6%259E%2590%2520url&rsv_pq=e65fc0b900033001&rsv_t=9a32aAdslq6MuK9Ne77AODPiFaod2t0ID07TVZdPCoxpCAPUTbZOsF5UGsU&rqlang=cn&rsv_enter=1&inputT=9004&rsv_sug3=123&rsv_sug1=127&rsv_sug7=100&rsv_n=2&bs=%E6%90%9C%E7%B4%A2%E8%AF%8D%20%E8%A7%A3%E6%9E%90%20url",
                "https://www.google.com/search?source=hp&ei=xS7XWr_oIpPWjwPyn6KoCw&btnG=Google+%E6%90%9C%E7%B4%A2&q=URL%E5%85%B3%E9%94%AE%E8%AF%8D%E6%8F%90%E5%8F%96",
                "https://www.google.com.hk/search?safe=strict&source=hp&ei=OS_XWrGOOYOAjwOXp7SICg&q=URL%E5%85%B3%E9%94%AE%E8%AF%8D%E6%8F%90%E5%8F%96&oq=URL%E5%85%B3%E9%94%AE%E8%AF%8D%E6%8F%90%E5%8F%96&gs_l=psy-ab.3...897.897.0.1208.1.1.0.0.0.0.269.269.2-1.1.0....0...1c.1.64.psy-ab..0.0.0....0.mwiXuEYDGlA",
                "https://www.sogou.com/web?query=URL%E5%85%B3%E9%94%AE%E8%AF%8D%E6%8F%90%E5%8F%96&_asf=www.sogou.com&_ast=&w=01019900&p=40040100&ie=utf8&from=index-nologin&s_from=index&sut=32043&sst0=1524051215324&lkt=0%2C0%2C0&sugsuv=004F632473ECA1435A5EDF544C219854&sugtime=1524051215324",
                "https://www.so.com/s?ie=utf-8&fr=none&src=360sou_newhome&q=URL%E5%85%B3%E9%94%AE%E8%AF%8D%E6%8F%90%E5%8F%96",
                "https://www.bing.com/search?q=URL%E5%85%B3%E9%94%AE%E8%AF%8D%E6%8F%90%E5%8F%96&qs=n&form=QBLHCN&sp=-1&pq=url%E5%85%B3%E9%94%AE%E8%AF%8D%E6%8F%90%E5%8F%96&sc=0-0&sk=&cvid=FD62E5F3A6234EC7A7E70138FC4519B3",
                "https://cn.bing.com/search?q=URL%E5%85%B3%E9%94%AE%E8%AF%8D%E6%8F%90%E5%8F%96&qs=n&form=QBLHCN&sp=-1&pq=url%E5%85%B3%E9%94%AE%E8%AF%8D%E6%8F%90%E5%8F%96&sc=0-0&sk=&cvid=FD62E5F3A6234EC7A7E70138FC4519B3",
                "http://www.youdao.com/w/eng/URL关键词提取/#keyfrom=dict2.index",
                // 图片
                "https://image.baidu.com/search/index?tn=baiduimage&ipn=r&ct=201326592&cl=2&lm=-1&st=-1&fm=detail&fr=&hs=0&xthttps=111111&sf=1&fmq=1524051826977_R&pv=&ic=0&nc=1&z=&se=&showtab=0&fb=0&width=&height=&face=0&istype=2&ie=utf-8&word=URL%E5%85%B3%E9%94%AE%E8%AF%8D%E6%8F%90%E5%8F%96&oq=URL%E5%85%B3%E9%94%AE%E8%AF%8D%E6%8F%90%E5%8F%96&rsp=-1",
                // 购物
                "https://s.taobao.com/search?q=URL%E5%85%B3%E9%94%AE%E8%AF%8D%E6%8F%90%E5%8F%96&imgfile=&commend=all&ssid=s5-e&search_type=item&sourceId=tb.index&spm=a21bo.2017.201856-taobao-item.1&ie=utf8&initiative_id=tbindexz_20170306",
                "https://list.tmall.com/search_product.htm?q=URL%E5%85%B3%E9%94%AE%E8%AF%8D%E6%8F%90%E5%8F%96&type=p&vmarket=&spm=875.7931836%2FB.a2227oh.d100&from=mallfp..pc_1_searchbutton",
                "https://search.jd.com/Search?keyword=URL%E5%85%B3%E9%94%AE%E8%AF%8D%E6%8F%90%E5%8F%96&enc=utf-8&pvid=b291164f4bd9417d84231a67d4b8aefc",
                "https://www.amazon.cn/s/ref=nb_sb_noss?__mk_zh_CN=%E4%BA%9A%E9%A9%AC%E9%80%8A%E7%BD%91%E7%AB%99&url=search-alias%3Daps&field-keywords=URL%E5%85%B3%E9%94%AE%E8%AF%8D%E6%8F%90%E5%8F%96",
                "http://search.dangdang.com/?key=URL%B9%D8%BC%FC%B4%CA%CC%E1%C8%A1&act=input&noresult=1",
                "https://search.suning.com/URL关键词提取/",
                "http://search.gome.com.cn/search?question=URL%E5%85%B3%E9%94%AE%E8%AF%8D%E6%8F%90%E5%8F%96&searchType=goods",
                // 其它
                "http://weixin.sogou.com/weixin?type=2&query=URL%E5%85%B3%E9%94%AE%E8%AF%8D%E6%8F%90%E5%8F%96&ie=utf8&s_from=input&_sug_=n&_sug_type_=",
                "http://s.weibo.com/weibo/URL%25E5%2585%25B3%25E9%2594%25AE%25E8%25AF%258D%25E6%258F%2590%25E5%258F%2596&Refer=index"
        };
        for (String url : urls) {
            System.out.println(url);
            Assert.assertEquals(sKey, KeyWordExtract.extract(url));
        }
    }


    @Test
    public void testOnlineRule() throws IOException {
        KeyWordExtract.loadOnlineRules("https://raw.githubusercontent.com/gudaoxuri/keyword-extract/master/src/main/resources/kwe-rules.txt");
        testAll();
    }
}
