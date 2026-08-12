package com.example.bookmanager.controller;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.Book;
import com.example.bookmanager.entity.BookQuote;
import com.example.bookmanager.entity.User;
import com.example.bookmanager.repository.BookQuoteRepository;
import com.example.bookmanager.repository.BookRepository;
import com.example.bookmanager.repository.UserRepository;
import com.example.bookmanager.service.BookService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;
    private final BookRepository bookRepository;
    private final BookQuoteRepository bookQuoteRepository;
    private final UserRepository userRepository;

    public BookController(BookService bookService, BookRepository bookRepository, BookQuoteRepository bookQuoteRepository, UserRepository userRepository) {
        this.bookService = bookService;
        this.bookRepository = bookRepository;
        this.bookQuoteRepository = bookQuoteRepository;
        this.userRepository = userRepository;
    }

    private String getRole(HttpServletRequest request) {
        return (String) request.getAttribute("role");
    }

    private void checkAdmin(HttpServletRequest request) {
        if (!"ADMIN".equals(getRole(request))) {
            throw new RuntimeException("仅管理员可执行此操作");
        }
    }

    /**
     * 分页查询图书列表，支持关键词搜索（所有用户可查看）
     */
    @GetMapping
    public ApiResponse<Page<Book>> list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<Book> books = bookService.findAll(keyword, category, pageable);
        return ApiResponse.success(books);
    }

    /**
     * 查询单本图书
     */
    @GetMapping("/{id}")
    public ApiResponse<Book> getById(@PathVariable Long id) {
        return bookService.findById(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "图书不存在"));
    }

    /**
     * 新增图书（仅管理员）
     */
    @PostMapping
    public ApiResponse<Book> create(@RequestBody Book book, HttpServletRequest request) {
        checkAdmin(request);
        Book saved = bookService.save(book);
        return ApiResponse.success("新增成功", saved);
    }

    /**
     * 更新图书（仅管理员）
     */
    @PutMapping("/{id}")
    public ApiResponse<Book> update(@PathVariable Long id, @RequestBody Book book, HttpServletRequest request) {
        checkAdmin(request);
        Book updated = bookService.update(id, book);
        return ApiResponse.success("更新成功", updated);
    }

    /**
     * 删除图书（仅管理员）
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        checkAdmin(request);
        bookService.deleteById(id);
        return ApiResponse.success("删除成功", null);
    }

    /**
     * 图书盲盒：随机推荐一本图书
     */
    @GetMapping("/random")
    public ApiResponse<Book> randomBook() {
        long count = bookRepository.count();
        if (count == 0) {
            return ApiResponse.error(404, "暂无图书");
        }
        int randomIndex = (int) (Math.random() * count);
        Page<Book> page = bookRepository.findAll(PageRequest.of(randomIndex, 1));
        if (page.hasContent()) {
            return ApiResponse.success(page.getContent().get(0));
        }
        return ApiResponse.error(404, "暂无图书");
    }

    /**
     * 书籍占卜：随机抽取并返回幸运之书 + 占卜文案
     */
    @GetMapping("/fortune")
    public ApiResponse<Map<String, Object>> fortune() {
        long count = bookRepository.count();
        if (count == 0) {
            return ApiResponse.error(404, "暂无图书");
        }
        int randomIndex = (int) (Math.random() * count);
        Page<Book> page = bookRepository.findAll(PageRequest.of(randomIndex, 1));
        if (!page.hasContent()) {
            return ApiResponse.error(404, "暂无图书");
        }
        Book book = page.getContent().get(0);
        String fortune = generateFortune(book);
        Map<String, Object> result = new HashMap<>();
        result.put("book", book);
        result.put("fortune", fortune);
        return ApiResponse.success(result);
    }

    private String generateFortune(Book book) {
        String[] templates = {
            "你今日的幸运之书是《%s》，预示你将拥有静谧独处的夜晚，在文字中邂逅另一个自己。",
            "命运之轮停在了《%s》！今天适合放下手机，翻开这本书，你会发现意想不到的惊喜。",
            "书籍精灵对你说：读《%s》吧！它会在你需要的时候，给你最温柔的慰藉。",
            "《%s》为你而来！今天的一切困惑，都能在这本书中找到答案。",
            "今日运势：《%s》将点亮你的灵感，让你看见不一样的世界。",
            "天选之书《%s》！翻开它，就像打开一扇通往新世界的大门。",
            "《%s》在呼唤你！今天读它，你会收获远超预期的能量。",
            "宇宙给你发来推荐：《%s》——这本书和你今天的磁场完美契合。",
            "书神显灵！今日宜读《%s》，忌焦虑，忌刷手机，宜沉浸书海。",
            "今日书香：《%s》——翻开第一页，好运气就会来找你。"
        };
        int idx = (int) (Math.random() * templates.length);
        return String.format(templates[idx], book.getTitle());
    }

    /**
     * 获取最新书摘/短评（弹幕墙）
     */
    @GetMapping("/quotes")
    public ApiResponse<List<BookQuote>> latestQuotes(@RequestParam(defaultValue = "10") int limit) {
        Page<BookQuote> page = bookQuoteRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit));
        return ApiResponse.success(page.getContent());
    }

    /**
     * 发布书摘/短评
     */
    @PostMapping("/quotes")
    public ApiResponse<BookQuote> addQuote(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        if (username == null) {
            return ApiResponse.error(401, "请先登录");
        }
        String content = (String) body.get("content");
        Long bookId = body.get("bookId") != null ? Long.valueOf(body.get("bookId").toString()) : null;
        Boolean isAnonymous = body.get("isAnonymous") != null ? (Boolean) body.get("isAnonymous") : false;

        BookQuote quote = new BookQuote();
        quote.setContent(content);
        quote.setIsAnonymous(isAnonymous);
        if (bookId != null) {
            bookRepository.findById(bookId).ifPresent(quote::setBook);
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        quote.setUser(user);
        BookQuote saved = bookQuoteRepository.save(quote);
        return ApiResponse.success("发布成功", saved);
    }

    /**
     * 按封面颜色推荐图书
     */
    @GetMapping("/color/{color}")
    public ApiResponse<Book> recommendByColor(@PathVariable String color) {
        Page<Book> page = bookRepository.findByCoverColorIgnoreCase(color, PageRequest.of(0, 50));
        if (page.hasContent()) {
            int idx = (int) (Math.random() * page.getContent().size());
            return ApiResponse.success(page.getContent().get(idx));
        }
        // fallback: random book
        long count = bookRepository.count();
        if (count > 0) {
            int randomIndex = (int) (Math.random() * count);
            Page<Book> fallback = bookRepository.findAll(PageRequest.of(randomIndex, 1));
            if (fallback.hasContent()) {
                return ApiResponse.success(fallback.getContent().get(0));
            }
        }
        return ApiResponse.error(404, "暂无匹配图书");
    }

    /**
     * 封面AI解读：随机返回一条封面解读文案
     */
    @GetMapping("/{bookId}/interpretation")
    public ApiResponse<Map<String, String>> getCoverInterpretation(@PathVariable Long bookId) {
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book == null) {
            return ApiResponse.error(404, "图书不存在");
        }
        String interpretations = book.getCoverInterpretations();
        if (interpretations == null || interpretations.trim().isEmpty()) {
            // 根据封面颜色自动生成默认文案
            String color = book.getCoverColor();
            String[] defaults;
            if (color != null && color.contains("blue")) {
                defaults = new String[]{"这本书看起来有点忧郁，适合深夜阅读。", "蓝色的封面藏着深邃的故事，翻开它，你会发现不一样的自己。"};
            } else if (color != null && color.contains("red")) {
                defaults = new String[]{"热情的红色封面，这本书一定充满激情与力量！", "红色象征勇气，这本书会点燃你的斗志。"};
            } else if (color != null && color.contains("green")) {
                defaults = new String[]{"绿色的封面让人感到宁静，适合在午后慢慢品读。", "这本书散发着清新的气息，像春天的第一缕风。"};
            } else {
                defaults = new String[]{"这本书的封面设计很有品味，内容一定不会让你失望。", "书里藏着很多人物，你一定能在其中找到自己。", "这本书看起来像是一个老朋友，等待与你重逢。"};
            }
            int idx = (int) (Math.random() * defaults.length);
            Map<String, String> result = new HashMap<>();
            result.put("interpretation", defaults[idx]);
            result.put("color", color != null ? color : "unknown");
            return ApiResponse.success(result);
        }
        String[] parts = interpretations.split("\\|\\|\\|");
        int idx = (int) (Math.random() * parts.length);
        Map<String, String> result = new HashMap<>();
        result.put("interpretation", parts[idx].trim());
        result.put("color", book.getCoverColor() != null ? book.getCoverColor() : "unknown");
        return ApiResponse.success(result);
    }
}