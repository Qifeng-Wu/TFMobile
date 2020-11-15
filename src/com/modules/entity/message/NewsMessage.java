package com.modules.entity.message;

/**
 * 图文消息
 * @author stephen
 *
 */
public class NewsMessage extends BaseMessage {  
	  //图文
    private News news;

    public News getNews() {
        return news;
    }

    public void setNews(News news) {
        this.news = news;
    }
} 