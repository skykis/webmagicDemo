package com.test.webmagic.processor;

import com.test.webmagic.model.Result;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.util.CollectionUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.pipeline.JsonFilePipeline;
import us.codecraft.webmagic.processor.PageProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GithubRepoPageProcessor implements PageProcessor {

    /**
     * 登录cookie，失效了需要重新网页登录替换
     */
    private String cookies = "IbXlEPbpCtNJ-kXdRNhi0CP6zq1H12GjPLIH4hQOEmfumKl3";

    private String githubUrl = "https://github.com";

    private Site site = Site.me().setRetryTimes(3).setSleepTime(3000)
            .setUserAgent(
                    "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36")
            .addCookie("user_session", cookies);

    @Override
    public void process(Page page) {
        // 定义如何抽取页面信息，并保存下来
        List<String> url = page.getHtml().xpath("//div[contains(@class, 'code-list-item')]/div[@class='d-flex']/div[contains(@class, 'flex-auto')]/a[2]/@href").all();
        if (CollectionUtils.isEmpty(url)) {
            //skip this page
            page.setSkip(true);
        }
        List<String> urls = url.stream().map(x -> githubUrl + x).collect(Collectors.toList());
        List<String> table = page.getHtml().xpath("//div[contains(@class, 'code-list-item')]/div[@class='file-box']/table/html()").all();
        String regEx_html = "<[^>]+>"; //定义HTML标签的正则表达式
        Pattern p_html = Pattern.compile(regEx_html, Pattern.CASE_INSENSITIVE);
        List<String> tables = table.stream().
                map(x ->
                        StringEscapeUtils.unescapeHtml4(
                                p_html.matcher(x.trim())
                                .replaceAll("")
                                .replace("\n", "")
                                .replaceAll("[ 0-9 ]", " ")
                                .replaceAll("\\s+", " ")
                        )
                )
                .collect(Collectors.toList());
        List<Result> results = new ArrayList<>();
        for (int i = 0; i < urls.size(); i++) {
            Result r = new Result();
            r.setUrl(urls.get(i));
            r.setTable(tables.get(i));
            results.add(r);
        }
        page.putField("url", page.getUrl());
        page.putField("result", results);
        // 从页面发现后续的url地址来抓取
        // List<String> targetRequest = page.getHtml().xpath("//*[@id=\"code_search_results\"]/div[2]/div/a[@class='next_page']/@href").all();
        // List<String> targetRequests = targetRequest.stream().map(x -> githubUrl + x).collect(Collectors.toList());
        // page.addTargetRequests(targetRequests);
        // 只抓第一页，人工分析
    }

    @Override
    public Site getSite() {
        return site;
    }

    public static void main(String[] args) {
        // 保存路径
        String filePath = "D:\\webmagic\\";
        // 搜索关键字
        String[] keyword = {"keyword"};
        for (String s : keyword) {
            Spider.create(new GithubRepoPageProcessor())
                    .addUrl("https://github.com/search?q=" + s + "&type=Code")
                    .addPipeline(new JsonFilePipeline(filePath + s + "\\"))
                    .thread(3)
                    .run();
        }
    }
}
