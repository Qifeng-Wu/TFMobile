package com.modules.entity.message;

import java.util.List;
/**
 * 图文
 * @author stephen
 *
 */
public class News {
     //文章列表
     private List<Article> articles;

    public List<Article> getArticles() {
        return articles;
    }

    public void setArticles(List<Article> articles) {
        this.articles = articles;
    }
     
     
}