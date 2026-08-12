package com.example.bookmanager.config;

import com.example.bookmanager.entity.*;
import com.example.bookmanager.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final BookRepository bookRepository;
    private final BadgeRepository badgeRepository;
    private final BlindBoxRepository blindBoxRepository;
    private final AuctionRepository auctionRepository;

    public DataInitializer(BookRepository bookRepository, BadgeRepository badgeRepository,
                           BlindBoxRepository blindBoxRepository, AuctionRepository auctionRepository) {
        this.bookRepository = bookRepository;
        this.badgeRepository = badgeRepository;
        this.blindBoxRepository = blindBoxRepository;
        this.auctionRepository = auctionRepository;
    }

    @Override
    public void run(String... args) {
        if (bookRepository.count() == 0) {
            List<Book> books = List.of(
                    createBook("活着", "余华", "9787530215319", "文学", "北京十月文艺出版社", 191,
                            "讲述了农村人福贵悲惨的人生遭遇。福贵本是个阔少爷，因嗜赌如命败光了家产。",
                            "",
                            LocalDate.of(2017, 6, 1)),

                    createBook("三体", "刘慈欣", "9787536692930", "科幻", "重庆出版社", 302,
                            "文化大革命如火如荼进行的同时，军方探寻外星文明的绝秘计划「红岸工程」取得了突破性进展。",
                            "",
                            LocalDate.of(2008, 1, 1)),

                    createBook("百年孤独", "加西亚·马尔克斯", "9787544253994", "文学", "南海出版公司", 360,
                            "《百年孤独》是魔幻现实主义文学的代表作，描写了布恩迪亚家族七代人的传奇故事。",
                            "",
                            LocalDate.of(2011, 6, 1)),

                    createBook("红楼梦", "曹雪芹", "9787020002207", "古典文学", "人民文学出版社", 1606,
                            "以贾宝玉、林黛玉、薛宝钗之间的恋爱婚姻悲剧为主线，描写了以贾家为代表的四大家族的兴衰。",
                            "",
                            LocalDate.of(1996, 12, 1)),

                    createBook("1984", "乔治·奥威尔", "9787530210291", "小说", "北京十月文艺出版社", 304,
                            "一部杰出的政治寓言小说，也是一部幻想小说。作品刻画了人类在极权主义社会的生存状态。",
                            "",
                            LocalDate.of(2010, 10, 1)),

                    createBook("Java编程思想", "Bruce Eckel", "9787111213826", "计算机", "机械工业出版社", 880,
                            "本书赢得了全球程序员的广泛赞誉，从Java的基础语法到最高级特性，都能指导你一步步掌握。",
                            "",
                            LocalDate.of(2007, 6, 1)),

                    createBook("人类简史", "尤瓦尔·赫拉利", "9787508647357", "历史", "中信出版社", 440,
                            "从十万年前有生命迹象开始到21世纪资本、科技交织的人类发展史，理清影响人类发展的重大脉络。",
                            "",
                            LocalDate.of(2014, 11, 1)),

                    createBook("小王子", "安托万·德·圣-埃克苏佩里", "9787020042494", "文学", "人民文学出版社", 97,
                            "以一位飞行员作为故事叙述者，讲述了小王子从自己星球出发前往地球的过程中，所经历的各种历险。",
                            "",
                            LocalDate.of(2003, 8, 1)),

                    createBook("围城", "钱钟书", "9787020024759", "小说", "人民文学出版社", 359,
                            "是中国现代文学史上一部风格独特的讽刺小说。被誉为「新儒林外史」。",
                            "",
                            LocalDate.of(1991, 2, 1)),

                    createBook("算法导论", "Thomas H. Cormen", "9787111407010", "计算机", "机械工业出版社", 1312,
                            "全书选材经典、内容丰富、结构合理、逻辑清晰，是计算机科学领域最为经典的算法教材之一。",
                            "",
                            LocalDate.of(2013, 1, 1))
            );

            bookRepository.saveAll(books);
            System.out.println("已插入 " + books.size() + " 条示例图书数据");
        }

        // 为已有图书设置默认售价（如果为空）
        List<Book> allBooks = bookRepository.findAll();
        boolean updated = false;
        for (Book book : allBooks) {
            if (book.getSalePrice() == null) {
                book.setSalePrice(29.9 + (int)(Math.random() * 100));
                book.setSaleableCopies(book.getSaleableCopies() != null ? book.getSaleableCopies() : 2);
                book.setDiscount(book.getDiscount() != null ? book.getDiscount() : 1.0);
                updated = true;
            }
        }
        if (updated) {
            bookRepository.saveAll(allBooks);
            System.out.println("已为图书设置默认售价");
        }

        // 为已有图书创建盲盒数据
        if (blindBoxRepository.count() == 0) {
            List<Book> booksForBlindBox = bookRepository.findAll();
            for (int i = 0; i < Math.min(booksForBlindBox.size(), 8); i++) {
                Book book = booksForBlindBox.get(i);
                BlindBox box = new BlindBox();
                box.setBook(book);
                box.setPrice(29.9);
                box.setCategory(book.getCategory());
                box.setStatus("ACTIVE");
                blindBoxRepository.save(box);
                // 每本书创建2个盲盒
                BlindBox box2 = new BlindBox();
                box2.setBook(book);
                box2.setPrice(29.9);
                box2.setCategory(book.getCategory());
                box2.setStatus("ACTIVE");
                blindBoxRepository.save(box2);
            }
            System.out.println("已创建 " + (Math.min(booksForBlindBox.size(), 8) * 2) + " 个盲盒数据");
        }

        // 创建拍卖示例数据
        if (auctionRepository.count() == 0) {
            List<Book> auctionBooks = bookRepository.findAll();
            if (auctionBooks.size() >= 3) {
                Auction a1 = new Auction();
                a1.setBook(auctionBooks.get(0));
                a1.setStartPrice(9.9);
                a1.setCurrentPrice(9.9);
                a1.setMinIncrement(2.0);
                a1.setStartTime(LocalDateTime.now());
                a1.setEndTime(LocalDateTime.now().plusHours(24));
                a1.setStatus("ACTIVE");
                auctionRepository.save(a1);

                Auction a2 = new Auction();
                a2.setBook(auctionBooks.get(1));
                a2.setStartPrice(19.9);
                a2.setCurrentPrice(19.9);
                a2.setMinIncrement(3.0);
                a2.setStartTime(LocalDateTime.now());
                a2.setEndTime(LocalDateTime.now().plusHours(48));
                a2.setStatus("ACTIVE");
                auctionRepository.save(a2);

                Auction a3 = new Auction();
                a3.setBook(auctionBooks.get(2));
                a3.setStartPrice(5.0);
                a3.setCurrentPrice(5.0);
                a3.setMinIncrement(1.0);
                a3.setStartTime(LocalDateTime.now().minusHours(2));
                a3.setEndTime(LocalDateTime.now().minusHours(1));
                a3.setStatus("ENDED");
                auctionRepository.save(a3);

                System.out.println("已创建 3 个拍卖示例数据");
            }
        }

        if (badgeRepository.count() == 0) {
            List<Badge> badges = List.of(
                    createBadge("first_borrow", "初次借阅", "完成第一次图书借阅", "📚", "BORROW_COUNT", 1),
                    createBadge("borrow_5", "借阅达人", "累计借阅满5本图书", "📖", "BORROW_COUNT", 5),
                    createBadge("borrow_10", "阅读狂人", "累计借阅满10本图书", "🏆", "BORROW_COUNT", 10),
                    createBadge("checkin_5", "坚持阅读", "连续签到5天", "🔥", "CHECKIN_STREAK", 5),
                    createBadge("checkin_10", "阅读习惯养成", "连续签到10天", "💪", "CHECKIN_STREAK", 10),
                    createBadge("checkin_total_30", "月度打卡王", "累计打卡30天", "🌟", "CHECKIN_TOTAL", 30),
                    createBadge("first_review", "初露锋芒", "发表第一篇书评", "✍️", "REVIEW_COUNT", 1),
                    createBadge("review_likes_10", "人气书评家", "书评累计获得10个赞", "❤️", "REVIEW_LIKES", 10)
            );
            badgeRepository.saveAll(badges);
            System.out.println("已插入 " + badges.size() + " 个徽章数据");
        }
    }

    private Badge createBadge(String code, String name, String description, String icon, String triggerType, Integer triggerValue) {
        Badge badge = new Badge();
        badge.setCode(code);
        badge.setName(name);
        badge.setDescription(description);
        badge.setIcon(icon);
        badge.setTriggerType(triggerType);
        badge.setTriggerValue(triggerValue);
        return badge;
    }

    private Book createBook(String title, String author, String isbn, String category,
                            String publisher, int pageCount, String description,
                            String imageUrl, LocalDate publicationDate) {
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setIsbn(isbn);
        book.setCategory(category);
        book.setPublisher(publisher);
        book.setPageCount(pageCount);
        book.setDescription(description);
        book.setImageUrl(imageUrl);
        book.setPublicationDate(publicationDate);
        book.setTotalCopies(3);
        book.setAvailableCopies(3);
        book.setSaleableCopies(2);
        book.setSalePrice(29.9 + (int)(Math.random() * 100));
        book.setDiscount(1.0);
        book.setStatus("AVAILABLE");
        return book;
    }
}