<template>
  <div class="dashboard" :class="'atmosphere-' + atmosphere">
    <!-- 今日书架氛围 -->
    <div class="atmosphere-bar" :class="'atmosphere-' + atmosphere">
      <span class="atmosphere-emoji">{{ atmosphereEmoji }}</span>
      <span class="atmosphere-text">{{ atmosphereMsg }}</span>
      <span class="atmosphere-count" v-if="todayBorrowCount > 0">
        今日借阅 {{ todayBorrowCount }} 次
      </span>
    </div>

    <!-- 公告栏 -->
    <div class="announcement-bar" v-if="announcements.length > 0">
      <div class="announce-scroll">
        <el-icon :size="18" color="#e6a23c"><Bell /></el-icon>
        <div class="announce-text-wrap">
          <div class="announce-text" v-for="(a, idx) in announcements" :key="a.id" :class="{ active: announceIdx === idx }">
            {{ a.title }}：{{ a.content?.substring(0, 60) }}{{ a.content?.length > 60 ? '...' : '' }}
          </div>
        </div>
      </div>
    </div>

    <!-- 轮播图 -->
    <el-carousel height="280px" :interval="5000" arrow="always" indicator-position="outside" class="carousel">
      <el-carousel-item v-for="(item, idx) in banners" :key="idx">
        <div class="banner-slide" :style="{ background: item.bg }">
          <div class="banner-text">
            <h2>{{ item.title }}</h2>
            <p>{{ item.desc }}</p>
          </div>
          <img :src="item.img" :alt="item.title" class="banner-img" />
        </div>
      </el-carousel-item>
    </el-carousel>

    <!-- 弹幕书摘墙 -->
    <div class="barrage-wall">
      <div class="barrage-title">
        <el-icon :size="18" color="#409eff"><ChatLineSquare /></el-icon>
        <span>书摘漂流瓶</span>
        <el-button size="small" text type="primary" @click="showQuoteInput = !showQuoteInput">
          <el-icon><Edit /></el-icon> 写一句
        </el-button>
      </div>
      <div class="barrage-track" v-if="quotes.length > 0">
        <div class="barrage-scroll">
          <span v-for="(q, i) in quotes" :key="i" class="barrage-item" :style="{ animationDelay: i * 3 + 's' }">
            「{{ q.content?.substring(0, 40) }}{{ q.content?.length > 40 ? '...' : '' }}」
            <small>{{ q.user?.nickname || q.user?.username || '匿名' }}</small>
          </span>
          <span v-for="(q, i) in quotes" :key="'dup-'+i" class="barrage-item" :style="{ animationDelay: i * 3 + 's' }">
            「{{ q.content?.substring(0, 40) }}{{ q.content?.length > 40 ? '...' : '' }}」
            <small>{{ q.user?.nickname || q.user?.username || '匿名' }}</small>
          </span>
        </div>
      </div>
      <div v-else class="barrage-empty">暂无书摘，快来写第一句吧 ~</div>
      <div v-if="showQuoteInput" class="quote-input-row">
        <el-input v-model="quoteContent" placeholder="写一句书摘或短评..." maxlength="200" show-word-limit />
        <el-button type="primary" size="small" @click="submitQuote" :loading="quoteSubmitting">发布</el-button>
      </div>
    </div>

    <!-- 趣味功能区 -->
    <div class="fun-zone">
      <!-- 书籍占卜 -->
      <div class="fun-card fortune-card" @click="handleFortune" :class="{ flipping: fortuneLoading }">
        <div class="fun-card-inner" :class="{ revealed: fortuneRevealed }">
          <div class="fun-card-front">
            <div class="fun-icon">🔮</div>
            <h3>今日命运之书</h3>
            <p>点击占卜你的幸运之书</p>
          </div>
          <div class="fun-card-back" v-if="fortuneBook">
            <div class="fortune-book-cover" :style="{ background: coverColors[fortuneBook.id % coverColors.length] }">
              <span>{{ fortuneBook.title?.charAt(0) }}</span>
            </div>
            <h4>《{{ fortuneBook.title }}》</h4>
            <p class="fortune-text">{{ fortuneText }}</p>
            <el-button size="small" type="primary" @click.stop="handleBorrowFortune" :loading="fortuneBorrowing">借阅此书</el-button>
          </div>
        </div>
      </div>

      <!-- 穿搭推荐 -->
      <div class="fun-card mood-card">
        <div class="fun-icon">👗</div>
        <h3>书封配色推荐</h3>
        <p>选择你今天的心情</p>
        <div class="mood-picker">
          <span v-for="m in moods" :key="m.color" class="mood-dot"
            :style="{ background: m.bg }" :title="m.label"
            @click="handleMoodPick(m)"></span>
        </div>
        <div v-if="moodBook" class="mood-result">
          <span class="mood-book-name">推荐：《{{ moodBook.title }}》</span>
          <el-tag size="small" type="primary">{{ moodBook.author }}</el-tag>
        </div>
      </div>

      <!-- 天气荐书 -->
      <div class="fun-card weather-card" @click="handleWeather">
        <div class="fun-icon">{{ weatherIcon }}</div>
        <h3>天气联动荐书</h3>
        <p>{{ weatherDesc }}</p>
        <div v-if="weatherBook" class="weather-result">
          <span class="weather-book-name">推荐：《{{ weatherBook.title }}》</span>
          <el-tag size="small" type="success">{{ weatherBook.author }}</el-tag>
        </div>
        <el-button v-if="weatherBook" size="small" type="success" @click.stop="handleBorrow(weatherBook)" style="margin-top:8px">
          借阅此书
        </el-button>
      </div>
    </div>

    <!-- 今日阅读搭档 -->
    <div class="partner-card" v-if="partner" @click="partnerOpened = true">
      <div class="partner-envelope" :class="{ opened: partnerOpened }">
        <div class="partner-seal" v-if="!partnerOpened">🔥</div>
        <div class="partner-content">
          <div class="partner-header">
            <span class="partner-title">📮 今日阅读搭档</span>
            <span class="partner-date">{{ new Date().toLocaleDateString() }}</span>
          </div>
          <div class="partner-body">
            <el-avatar :size="50" class="partner-avatar">
              {{ (partner.partner?.nickname || partner.partner?.username || '?').charAt(0).toUpperCase() }}
            </el-avatar>
            <div class="partner-info">
              <span class="partner-name">{{ partner.partner?.nickname || partner.partner?.username }}</span>
              <span class="partner-common-books" v-if="partner.commonBooks">
                你们共同读过 {{ partner.commonBooks.split(',').length }} 本书
              </span>
            </div>
          </div>
          <el-button v-if="!partnerOpened" type="primary" size="small" @click.stop="partnerOpened = true">
            ✉️ 拆开信封
          </el-button>
          <div v-else class="partner-greeting">
            <p class="greeting-text">「我们一起读完了好书，聊聊感想吧！」</p>
            <el-button type="primary" size="small" @click.stop="handleGreetPartner" :loading="greetSending">
              👋 打招呼
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="main-content">
      <!-- 左侧：图书列表 -->
      <div class="content-left">
        <div class="section">
          <div class="section-header">
            <div class="section-title">
              <el-icon :size="22" :color="searchKeyword ? '#409eff' : '#e6a23c'">
                <TrendCharts />
              </el-icon>
              <span>{{ searchKeyword ? '搜索结果' : '热门图书' }}</span>
            </div>
            <div class="section-search">
              <el-select
                v-model="selectedCategory"
                placeholder="按分类筛选"
                clearable
                @change="fetchBooks"
                class="category-select"
              >
                <el-option
                  v-for="cat in categories"
                  :key="cat"
                  :label="cat"
                  :value="cat"
                />
              </el-select>
              <el-select
                v-model="sortBy"
                placeholder="排序方式"
                @change="fetchBooks"
                class="sort-select"
              >
                <el-option label="默认排序" value="id" />
                <el-option label="出版日期" value="publicationDate" />
                <el-option label="上架时间" value="createdAt" />
              </el-select>
              <el-button
                :icon="sortDir === 'asc' ? 'SortUp' : 'SortDown'"
                @click="toggleSortDir"
                class="sort-dir-btn"
                circle
                size="small"
              />
              <el-button
                type="warning"
                @click="handleBlindBox"
                :icon="Present"
                class="blind-box-btn"
                size="small"
              >
                今天读什么
              </el-button>
              <el-input
                v-model="searchKeyword"
                placeholder="搜索书名、作者、ISBN..."
                clearable
                prefix-icon="Search"
                @keyup.enter="fetchBooks"
                @clear="fetchBooks"
                class="search-input"
              />
              <VoiceSearch @search="onVoiceSearch" @command="onVoiceCommand" />
            </div>
          </div>

          <div v-loading="loading" class="book-grid">
            <el-empty v-if="!loading && books.length === 0" description="暂无图书" />
            <div
              v-for="book in books"
              :key="book.id"
              class="book-card"
              @click="handleViewDetail(book)"
            >
              <div class="book-cover" :style="{ background: coverColors[book.id % coverColors.length] }" @click.stop="handleCoverInterpret(book)">
                <span class="cover-char">{{ book.title?.charAt(0) }}</span>
                <div class="cover-hover-hint">点击查看AI解读</div>
                <div class="cover-badge" v-if="book.id <= 5">
                  <el-tag type="danger" size="small" effect="dark">热门</el-tag>
                </div>
              </div>
              <div class="book-info">
                <h4 class="book-name">{{ book.title }}</h4>
                <div class="book-meta">
                  <el-icon :size="14"><User /></el-icon>
                  <span>{{ book.author }}</span>
                </div>
                <div class="book-meta">
                  <el-tag size="small" v-if="book.category" type="primary">{{ book.category }}</el-tag>
                  <span v-else class="text-muted">未分类</span>
                </div>
                <div class="book-meta rating-meta" v-if="book.avgRating">
                  <el-rate v-model="book.avgRating" disabled size="small" show-score text-color="#f7ba2a" />
                  <span class="review-count">({{ book.reviewCount }})</span>
                </div>
                <div class="book-meta">
                  <span class="stock">借阅库存: {{ book.availableCopies || 0 }}/{{ book.totalCopies || 0 }}</span>
                  <span class="sale-stock">可售: {{ book.saleableCopies || 0 }}</span>
                </div>
                <div class="book-meta">
                  <span class="book-price">
                    <span class="price-current">¥{{ ((book.salePrice || 0) * (book.discount || 1)).toFixed(2) }}</span>
                    <span v-if="book.discount && book.discount < 1" class="price-original">¥{{ (book.salePrice || 0).toFixed(2) }}</span>
                    <el-tag v-if="book.discount && book.discount < 1" type="danger" size="small" effect="plain">{{ (book.discount * 10).toFixed(1) }}折</el-tag>
                  </span>
                </div>
                <div class="book-actions">
                  <el-button
                    v-if="(book.availableCopies || 0) > 0"
                    type="primary"
                    size="small"
                    @click.stop="handleBorrow(book)"
                  >
                    借阅
                  </el-button>
                  <el-button
                    v-else
                    type="warning"
                    size="small"
                    @click.stop="handleReserve(book)"
                  >
                    预约
                  </el-button>
                  <el-button
                    type="success"
                    size="small"
                    :disabled="(book.saleableCopies || 0) <= 0"
                    @click.stop="handleBuy(book)"
                    style="margin-left: 8px"
                  >
                    {{ (book.saleableCopies || 0) > 0 ? '购买' : '已售罄' }}
                  </el-button>
                  <el-button
                    :type="book.isFavorited ? 'warning' : 'default'"
                    size="small"
                    @click.stop="handleToggleFavorite(book)"
                    :icon="book.isFavorited ? 'StarFilled' : 'Star'"
                    :loading="book.favLoading"
                    style="margin-left: 8px"
                  >
                    {{ book.isFavorited ? '已收藏' : '收藏' }}
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 图书评价弹窗 -->
        <el-dialog v-model="viewDialogVisible" title="图书详情" width="640px">
          <div v-if="viewBook" class="detail-wrap">
            <div class="detail-cover" :style="{ background: coverColors[viewBook.id % coverColors.length] }">
              <span class="detail-cover-char">{{ viewBook.title?.charAt(0) }}</span>
            </div>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="书名" :span="2">{{ viewBook.title }}</el-descriptions-item>
              <el-descriptions-item label="作者">{{ viewBook.author }}</el-descriptions-item>
              <el-descriptions-item label="ISBN">{{ viewBook.isbn }}</el-descriptions-item>
              <el-descriptions-item label="分类">
                <el-tag size="small" type="primary">{{ viewBook.category || '未分类' }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="出版社">{{ viewBook.publisher || '-' }}</el-descriptions-item>
              <el-descriptions-item label="页数">{{ viewBook.pageCount ? viewBook.pageCount + ' 页' : '-' }}</el-descriptions-item>
              <el-descriptions-item label="出版日期">{{ viewBook.publicationDate || '-' }}</el-descriptions-item>
              <el-descriptions-item label="库存">{{ viewBook.availableCopies || 0 }}/{{ viewBook.totalCopies || 0 }}</el-descriptions-item>
              <el-descriptions-item label="售价">
                <span class="detail-price">
                  <span class="price-current">¥{{ ((viewBook.salePrice || 0) * (viewBook.discount || 1)).toFixed(2) }}</span>
                  <span v-if="viewBook.discount && viewBook.discount < 1" class="price-original">¥{{ (viewBook.salePrice || 0).toFixed(2) }}</span>
                  <el-tag v-if="viewBook.discount && viewBook.discount < 1" type="danger" size="small" effect="plain">{{ (viewBook.discount * 10).toFixed(1) }}折</el-tag>
                </span>
              </el-descriptions-item>
              <el-descriptions-item label="可售库存">{{ viewBook.saleableCopies || 0 }}</el-descriptions-item>
              <el-descriptions-item label="评分" v-if="viewBookRating">
                <el-rate v-model="viewBookRating" disabled show-score text-color="#f7ba2a" />
              </el-descriptions-item>
              <el-descriptions-item label="描述" :span="2">{{ viewBook.description || '暂无描述' }}</el-descriptions-item>
            </el-descriptions>

            <!-- 评价区域 -->
            <div class="review-section" v-if="viewBook">
              <el-divider>图书评价</el-divider>
              <div class="add-review" v-if="!myReview">
                <el-input v-model="reviewForm.title" placeholder="书评标题（可选）" maxlength="100" show-word-limit />
                <el-rate v-model="reviewForm.rating" :max="5" />
                <el-input
                  v-model="reviewForm.comment"
                  type="textarea"
                  :rows="3"
                  placeholder="写下你的书评或读后感..."
                  maxlength="2000"
                  show-word-limit
                />
                <el-button type="primary" size="small" @click="submitReview" :loading="reviewSubmitting" style="margin-top:8px">
                  提交评价
                </el-button>
              </div>
              <div v-else class="my-review">
                <el-alert title="我的评价" type="success" :closable="false">
                  <el-rate v-model="myReview.rating" disabled size="small" />
                  <p>{{ myReview.comment || '无评论' }}</p>
                </el-alert>
              </div>
              <div class="review-list" v-if="reviews.length > 0">
                <div v-for="r in reviews" :key="r.id" class="review-item">
                  <div class="review-user">
                    <span class="review-username">{{ r.user.nickname || r.user.username }}</span>
                    <el-rate v-model="r.rating" disabled size="small" />
                  </div>
                  <div class="review-title" v-if="r.title">{{ r.title }}</div>
                  <p class="review-comment">{{ r.comment || r.content || '无评论' }}</p>
                  <div class="review-footer">
                    <span class="review-time">{{ formatDateTime(r.createdAt) }}</span>
                    <span class="review-likes" v-if="r.likeCount > 0">👍 {{ r.likeCount }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <template #footer>
            <el-button @click="viewDialogVisible = false">关闭</el-button>
            <el-button type="success" :disabled="(viewBook.saleableCopies || 0) <= 0" @click="handleBuy(viewBook); viewDialogVisible = false">
              {{ (viewBook.saleableCopies || 0) > 0 ? '购买' : '已售罄' }}
            </el-button>
          </template>
        </el-dialog>

        <!-- 借阅弹窗 -->
        <el-dialog v-model="borrowDialogVisible" title="借阅图书" width="480px" top="5vh" :close-on-click-modal="false">
          <div v-if="borrowBookInfo" class="borrow-form">
            <div class="borrow-book-header">
              <div class="borrow-book-img-placeholder" :style="{ background: coverColors[borrowBookInfo.id % coverColors.length] }">
                <span class="borrow-cover-char">{{ borrowBookInfo.title?.charAt(0) }}</span>
              </div>
              <div class="borrow-book-meta">
                <h4>{{ borrowBookInfo.title }}</h4>
                <p>作者：{{ borrowBookInfo.author }}</p>
                <p>ISBN：{{ borrowBookInfo.isbn }}</p>
                <p>库存：{{ borrowBookInfo.availableCopies || 0 }}/{{ borrowBookInfo.totalCopies || 0 }}</p>
              </div>
            </div>
            <el-form :model="borrowForm" label-width="100px">
              <el-form-item>
                <el-alert v-if="borrowError" :title="borrowError" type="error" :closable="false" show-icon />
              </el-form-item>
              <el-form-item label="借阅日期">
                <el-date-picker
                  v-model="borrowForm.borrowDate"
                  type="date"
                  placeholder="选择借阅日期（默认今天）"
                  value-format="YYYY-MM-DD"
                  :disabled-date="disabledDate"
                />
              </el-form-item>
              <el-form-item label="归还日期">
                <el-date-picker
                  v-model="borrowForm.dueDate"
                  type="date"
                  placeholder="选择归还日期（默认30天后）"
                  value-format="YYYY-MM-DD"
                  :disabled-date="disabledDueDate"
                />
              </el-form-item>
            </el-form>
            <!-- 时空胶囊 -->
            <div class="capsule-section" v-if="capsuleMessage">
              <div class="capsule-label">📜 时空胶囊留言</div>
              <div class="capsule-content">「{{ capsuleMessage }}」</div>
              <span class="capsule-hint">来自上一位读者的留言</span>
            </div>
          </div>
          <template #footer>
            <el-button @click="borrowDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="confirmBorrow" :loading="borrowSubmitting">确认借阅</el-button>
          </template>
        </el-dialog>

        <!-- 预约弹窗 -->
        <el-dialog v-model="reserveDialogVisible" title="预约图书" width="480px" top="5vh" :close-on-click-modal="false">
          <div v-if="reserveBookInfo" class="borrow-form">
            <div class="borrow-book-header">
              <div class="borrow-book-img-placeholder" :style="{ background: coverColors[reserveBookInfo.id % coverColors.length] }">
                <span class="borrow-cover-char">{{ reserveBookInfo.title?.charAt(0) }}</span>
              </div>
              <div class="borrow-book-meta">
                <h4>{{ reserveBookInfo.title }}</h4>
                <p>作者：{{ reserveBookInfo.author }}</p>
                <p>ISBN：{{ reserveBookInfo.isbn }}</p>
                <p>库存：0/{{ reserveBookInfo.totalCopies || 0 }}</p>
              </div>
            </div>
            <el-alert
              v-if="earliestReturnDate"
              :title="`预计最早可取书日期：${formatDateStr(earliestReturnDate)}`"
              type="info"
              :closable="false"
              show-icon
              style="margin-bottom:16px"
            />
            <el-alert
              v-else
              title="暂无借出记录，请等待图书归还"
              type="warning"
              :closable="false"
              show-icon
              style="margin-bottom:16px"
            />
          </div>
          <template #footer>
            <el-button @click="reserveDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="confirmReserve" :loading="reserveSubmitting">确认预约</el-button>
          </template>
        </el-dialog>
      </div>

      <!-- 右侧：排行榜 -->
      <div class="content-right">
        <el-card shadow="hover" class="ranking-card">
          <template #header>
            <div class="ranking-header">
              <el-icon :size="18" color="#e6a23c"><TrophyBase /></el-icon>
              <span>本月借阅排行</span>
            </div>
          </template>
          <div v-if="rankings.length === 0" class="ranking-empty">
            <span>暂无数据</span>
          </div>
          <div v-for="(item, idx) in rankings" :key="idx" class="ranking-item">
            <div class="rank-num" :class="{ top3: idx < 3 }">{{ idx + 1 }}</div>
            <div class="rank-info">
              <span class="rank-title">{{ item.title }}</span>
              <span class="rank-meta">{{ item.author }} | {{ item.category || '未分类' }}</span>
            </div>
            <span class="rank-count">{{ item.count }}次</span>
            <el-button type="primary" size="small" link @click="handleRankingBorrow(item)" v-if="item.bookId">
              借阅
            </el-button>
          </div>
        </el-card>
      </div>
    </div>
  <!-- 盲盒弹窗 -->
    <el-dialog v-model="blindBoxVisible" title="今天读什么？" width="520px" :close-on-click-modal="false">
      <div class="blind-box-container">
        <div v-if="!blindBoxRevealed" class="blind-box-animation">
          <div class="gift-box" :class="{ shaking: blindBoxLoading }">
            <div class="gift-box-lid"></div>
            <div class="gift-box-body">
              <span class="question-mark">?</span>
            </div>
          </div>
          <p v-if="blindBoxLoading" class="blind-box-text">正在为你挑选好书...</p>
        </div>
        <div v-else-if="blindBoxBook" class="blind-box-result">
          <div class="result-book-cover" :style="{ background: coverColors[blindBoxBook.id % coverColors.length] }">
            <span class="result-cover-char">{{ blindBoxBook.title?.charAt(0) }}</span>
          </div>
          <h3>{{ blindBoxBook.title }}</h3>
          <p class="result-author">作者：{{ blindBoxBook.author }}</p>
          <p class="result-category">
            <el-tag size="small" type="primary">{{ blindBoxBook.category || '未分类' }}</el-tag>
          </p>
          <p class="result-desc">{{ blindBoxBook.description?.substring(0, 100) }}{{ blindBoxBook.description?.length > 100 ? '...' : '' }}</p>
          <div style="display:flex;gap:12px;justify-content:center;margin-top:16px">
            <el-button type="primary" @click="blindBoxVisible = false; handleBorrow(blindBoxBook)" v-if="(blindBoxBook.availableCopies || 0) > 0">
              立即借阅
            </el-button>
            <el-button type="warning" @click="blindBoxVisible = false; handleReserve(blindBoxBook)" v-else>
              预约此书
            </el-button>
            <el-button @click="handleBlindBox">再抽一本</el-button>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- AI封面解读弹窗 -->
    <el-dialog v-model="interpretationVisible" title="🤖 AI第一印象" width="420px" top="15vh" :close-on-click-modal="true">
      <div class="interpretation-dialog" v-if="interpretationBook">
        <div class="interpretation-cover" :style="{ background: coverColors[interpretationBook.id % coverColors.length] }">
          <span class="interpretation-char">{{ interpretationBook.title?.charAt(0) }}</span>
        </div>
        <h3 class="interpretation-title">{{ interpretationBook.title }}</h3>
        <p class="interpretation-author">{{ interpretationBook.author }}</p>
        <div class="interpretation-bubble">
          <p>{{ interpretationText }}</p>
        </div>
        <el-button type="primary" plain @click="interpretationVisible = false; handleViewDetail(interpretationBook)">
          查看详情
        </el-button>
      </div>
    </el-dialog>

    <!-- 购买成功弹窗 -->
    <el-dialog v-model="buySuccessVisible" title="购买成功" width="420px" top="20vh" :close-on-click-modal="false" center append-to-body>
      <div class="buy-success-body">
        <div class="buy-success-icon">🎉</div>
        <p class="buy-success-text">《{{ buySuccessBook?.title }}》已成功加入购物车！</p>
        <p class="buy-success-hint">请前往购物车进行结算。</p>
      </div>
      <template #footer>
        <el-button @click="buySuccessVisible = false">继续浏览</el-button>
        <el-button type="primary" @click="buySuccessVisible = false; router.push('/cart')">前往购物车</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Reading, User, Document, Calendar, TrendCharts, Bell, StarFilled, Star, TrophyBase, Present, SortUp, SortDown, ChatLineSquare, Edit } from '@element-plus/icons-vue'
import { getBooks, getFortuneBook, getLatestQuotes, addBookQuote, recommendByColor, getCoverInterpretation } from '../api/book'
import { borrowBook, reserveBook, getEarliestReturnDate, weatherRecommend, getRandomCapsule, getTodayStats, getTodayPartner } from '../api/borrow'
import { addFavorite, removeFavorite, isFavorited, getBookReviews, getBookRatingStats, addReview, addFullReview } from '../api/social'
import { getLatestAnnouncements } from '../api/announcement'
import { getMonthlyRanking, getCategories, getRandomBook } from '../api/stats'
import { sendMessage } from '../api/community'
import { addToCart } from '../api/order'
import VoiceSearch from '../components/VoiceSearch.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const books = ref([])
const searchKeyword = ref('')
const selectedCategory = ref('')
const sortBy = ref('id')
const sortDir = ref('asc')
const categories = ref([])

const viewDialogVisible = ref(false)
const viewBook = ref(null)
const viewBookRating = ref(0)
const reviews = ref([])
const myReview = ref(null)
const reviewForm = ref({ title: '', rating: 0, comment: '' })
const reviewSubmitting = ref(false)

const borrowDialogVisible = ref(false)
const borrowBookInfo = ref(null)
const borrowError = ref('')
const borrowSubmitting = ref(false)
const borrowForm = ref({ borrowDate: '', dueDate: '' })

const reserveDialogVisible = ref(false)
const reserveBookInfo = ref(null)
const reserveSubmitting = ref(false)
const earliestReturnDate = ref(null)
const capsuleMessage = ref(null)

const buySuccessVisible = ref(false)
const buySuccessBook = ref(null)

const rankings = ref([])
const announcements = ref([])
const announceIdx = ref(0)

let announceTimer = null

// 弹幕书摘
const quotes = ref([])
const showQuoteInput = ref(false)
const quoteContent = ref('')
const quoteSubmitting = ref(false)

// 书籍占卜
const fortuneLoading = ref(false)
const fortuneRevealed = ref(false)
const fortuneBook = ref(null)
const fortuneBookId = ref(null)
const fortuneText = ref('')
const fortuneBorrowing = ref(false)

// 穿搭推荐
const moodBook = ref(null)
const moods = [
  { label: '热情红', color: 'red', bg: 'linear-gradient(135deg, #f5576c, #f093fb)' },
  { label: '宁静蓝', color: 'blue', bg: 'linear-gradient(135deg, #4facfe, #00f2fe)' },
  { label: '清新绿', color: 'green', bg: 'linear-gradient(135deg, #43e97b, #38f9d7)' },
  { label: '神秘紫', color: 'purple', bg: 'linear-gradient(135deg, #667eea, #764ba2)' },
  { label: '温暖橙', color: 'orange', bg: 'linear-gradient(135deg, #fa709a, #fee140)' },
  { label: '梦幻粉', color: 'pink', bg: 'linear-gradient(135deg, #a18cd1, #fbc2eb)' },
]

// 天气荐书
const weatherBook = ref(null)
const weatherIcon = ref('☀️')
const weatherDesc = ref('点击获取今日天气荐书')
const weatherMap = {
  '晴': { icon: '☀️', desc: '今天阳光明媚，适合读一本轻松的小说' },
  '雨': { icon: '🌧️', desc: '窗外下雨，不如窝在沙发读一本悬疑故事' },
  '雪': { icon: '❄️', desc: '雪花飘落，捧一本温暖的书度过午后' },
  '云': { icon: '☁️', desc: '多云天气，适合读一本治愈系散文' },
  '风': { icon: '🌬️', desc: '微风轻拂，来一本热血冒险小说吧' },
}

// 今日书架氛围
const atmosphere = ref('quiet')
const atmosphereEmoji = ref('🌙')
const atmosphereMsg = ref('图书馆静悄悄的，等你来翻开第一页。')
const todayBorrowCount = ref(0)
const atmosphereTimer = ref(null)

// 阅读搭档
const partner = ref(null)
const partnerOpened = ref(false)
const greetSending = ref(false)

// AI解读
const interpretationVisible = ref(false)
const interpretationText = ref('')
const interpretationBook = ref(null)

const coverColors = [
  'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
  'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
  'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
  'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
  'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
  'linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)',
  'linear-gradient(135deg, #fccb90 0%, #d57eeb 100%)',
  'linear-gradient(135deg, #e0c3fc 0%, #8ec5fc 100%)',
]

const banners = [
  {
    title: '欢迎使用图书管理系统',
    desc: '海量图书资源，一键借阅管理',
    bg: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    img: 'https://images.unsplash.com/photo-1507842217343-583bb7270b66?w=800&q=80'
  },
  {
    title: '发现你的下一本好书',
    desc: '探索精彩纷呈的图书世界',
    bg: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
    img: 'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=800&q=80'
  },
  {
    title: '知识改变命运',
    desc: '每天阅读一小时，遇见更好的自己',
    bg: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    img: 'https://images.unsplash.com/photo-1519682337058-a94d519337bc?w=800&q=80'
  },
]

function formatDateStr(date) {
  if (!date) return ''
  const d = new Date(date)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function formatDateTime(date) {
  if (!date) return '-'
  return formatDateStr(date) + ' ' + new Date(date).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function disabledDate(time) {
  return time.getTime() > Date.now()
}

function disabledDueDate(time) {
  if (borrowForm.value.borrowDate) {
    return time.getTime() < new Date(borrowForm.value.borrowDate).getTime()
  }
  return false
}

async function handleViewDetail(book) {
  viewBook.value = book
  viewDialogVisible.value = true
  viewBookRating.value = 0
  reviews.value = []
  myReview.value = null
  reviewForm.value = { title: '', rating: 0, comment: '' }

  // 获取评分统计
  try {
    const statsRes = await getBookRatingStats(book.id)
    viewBookRating.value = statsRes.data.averageRating || 0
  } catch (e) { /* handled */ }

  // 获取评价列表
  try {
    const revRes = await getBookReviews(book.id, { page: 0, size: 10 })
    reviews.value = revRes.data.content
    // 检查是否有自己的评价
    const username = localStorage.getItem('username')
    myReview.value = revRes.data.content.find(r => r.user.username === username) || null
  } catch (e) { /* handled */ }
}

async function submitReview() {
  if (!reviewForm.value.rating || reviewForm.value.rating === 0) {
    ElMessage.warning('请选择评分')
    return
  }
  reviewSubmitting.value = true
  try {
    await addFullReview(viewBook.value.id, {
      title: reviewForm.value.title,
      content: reviewForm.value.comment,
      rating: reviewForm.value.rating
    })
    ElMessage.success('评价成功')
    handleViewDetail(viewBook.value)
  } catch (e) {
    // handled
  } finally {
    reviewSubmitting.value = false
  }
}

async function handleToggleFavorite(book) {
  book.favLoading = true
  try {
    if (book.isFavorited) {
      await removeFavorite(book.id)
      book.isFavorited = false
      ElMessage.success('已取消收藏')
    } else {
      await addFavorite(book.id)
      book.isFavorited = true
      ElMessage.success('收藏成功')
    }
  } catch (e) {
    // handled
  } finally {
    book.favLoading = false
  }
}

async function handleBorrow(book) {
  borrowError.value = ''
  borrowBookInfo.value = book
  borrowForm.value = { borrowDate: '', dueDate: '' }
  borrowDialogVisible.value = true
  capsuleMessage.value = null
}

async function handleBuy(book) {
  try {
    await addToCart(book.id, 1)
    buySuccessBook.value = book
    buySuccessVisible.value = true
  } catch (e) {
    // error shown by interceptor
  }
}

function onVoiceSearch(keyword) {
  searchKeyword.value = keyword
  fetchBooks()
}

function onVoiceCommand(cmd) {
  if (cmd === 'borrow') router.push('/borrows')
  else if (cmd === 'recommend') { searchKeyword.value = ''; fetchBooks() }
  else if (cmd === 'blindbox') router.push('/blind-box')
  else if (cmd === 'auction') router.push('/auctions')
}

async function confirmBorrow() {
  if (!borrowBookInfo.value) return
  borrowError.value = ''
  borrowSubmitting.value = true
  try {
    const body = {}
    if (borrowForm.value.borrowDate) {
      body.borrowDate = borrowForm.value.borrowDate + 'T00:00:00'
    }
    if (borrowForm.value.dueDate) {
      body.dueDate = borrowForm.value.dueDate + 'T00:00:00'
    }
    const res = await borrowBook(borrowBookInfo.value.id, body)
    ElMessage.success(res.message || '借阅成功')
    borrowDialogVisible.value = false
    fetchBooks()
  } catch (e) {
    // 在弹窗内显示错误信息
    const msg = e?.response?.data?.message || e?.message || '借阅失败'
    borrowError.value = msg
  } finally {
    borrowSubmitting.value = false
  }
}

async function handleReserve(book) {
  reserveBookInfo.value = book
  earliestReturnDate.value = null
  reserveDialogVisible.value = true
  getEarliestReturnDate(book.id)
    .then(res => { earliestReturnDate.value = res.data })
    .catch(() => {})
}

async function confirmReserve() {
  if (!reserveBookInfo.value) return
  reserveSubmitting.value = true
  try {
    const res = await reserveBook(reserveBookInfo.value.id)
    ElMessage.success(res.message || '预约成功')
    reserveDialogVisible.value = false
  } catch (e) {
    ElMessage.error('预约失败，请稍后重试')
  } finally {
    reserveSubmitting.value = false
  }
}

async function handleRankingBorrow(item) {
  // Find the book info for the dialog
  const book = books.value.find(b => b.id === item.bookId)
  if (book) {
    handleBorrow(book)
  } else {
    // Fallback: try direct borrow
    try {
      await borrowBook(item.bookId, {})
      ElMessage.success('借阅成功')
      fetchBooks()
      fetchRankings()
    } catch (e) {
      // handled
    }
  }
}

async function fetchBooks() {
    loading.value = true
    try {
      const params = {
        keyword: searchKeyword.value,
        category: selectedCategory.value,
        sortBy: sortBy.value,
        sortDir: sortDir.value,
        page: 0,
        size: 12
      }
      const res = await getBooks(params)
    const bookList = res.data.content

    // 获取收藏状态 + 评分
    const username = localStorage.getItem('username')
    if (username && bookList.length > 0) {
      const favPromises = bookList.map(b => isFavorited(b.id).then(r => ({ id: b.id, fav: r.data })).catch(() => ({ id: b.id, fav: false })))
      const ratingPromises = bookList.map(b => getBookRatingStats(b.id).then(r => ({ id: b.id, rating: r.data.averageRating, count: r.data.reviewCount })).catch(() => ({ id: b.id, rating: 0, count: 0 })))
      const [favResults, ratingResults] = await Promise.all([
        Promise.all(favPromises),
        Promise.all(ratingPromises)
      ])
      bookList.forEach(b => {
        const fav = favResults.find(f => f.id === b.id)
        const rat = ratingResults.find(r => r.id === b.id)
        b.isFavorited = fav ? fav.fav : false
        b.favLoading = false
        b.avgRating = rat ? rat.rating : 0
        b.reviewCount = rat ? rat.count : 0
      })
    }
    books.value = bookList
  } catch (e) {
    // handled
  } finally {
    loading.value = false
  }
}

async function fetchRankings() {
  try {
    const res = await getMonthlyRanking()
    rankings.value = res.data || []
  } catch (e) { /* handled */ }
}

async function fetchAnnouncements() {
  try {
    const res = await getLatestAnnouncements()
    announcements.value = res.data || []
  } catch (e) { /* handled */ }
}

async function fetchCategories() {
  try {
    const res = await getCategories()
    categories.value = res.data || []
  } catch (e) { /* handled */ }
}

function toggleSortDir() {
  sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
  fetchBooks()
}

/* 盲盒弹窗 */
const blindBoxVisible = ref(false)
const blindBoxBook = ref(null)
const blindBoxLoading = ref(false)
const blindBoxRevealed = ref(false)

async function handleBlindBox() {
  blindBoxVisible.value = true
  blindBoxRevealed.value = false
  blindBoxLoading.value = true
  blindBoxBook.value = null
  await new Promise(r => setTimeout(r, 1500))
  try {
    const res = await getRandomBook()
    blindBoxBook.value = res.data
    blindBoxRevealed.value = true
  } catch (e) {
    // handled
  } finally {
    blindBoxLoading.value = false
  }
}

// 弹幕书摘
async function fetchQuotes() {
  try {
    const res = await getLatestQuotes(10)
    quotes.value = res.data || []
  } catch (e) { /* handled */ }
}

async function submitQuote() {
  if (!quoteContent.value.trim()) return
  quoteSubmitting.value = true
  try {
    await addBookQuote({ content: quoteContent.value.trim(), isAnonymous: false })
    ElMessage.success('发布成功')
    quoteContent.value = ''
    showQuoteInput.value = false
    fetchQuotes()
  } catch (e) { /* handled */ } finally {
    quoteSubmitting.value = false
  }
}

// 书籍占卜
async function handleFortune() {
  if (fortuneLoading.value) return
  fortuneLoading.value = true
  fortuneRevealed.value = false
  fortuneBook.value = null
  await new Promise(r => setTimeout(r, 1200))
  try {
    const res = await getFortuneBook()
    fortuneBook.value = res.data.book
    fortuneBookId.value = res.data.book.id
    fortuneText.value = res.data.fortune
    fortuneRevealed.value = true
  } catch (e) { /* handled */ } finally {
    fortuneLoading.value = false
  }
}

async function handleBorrowFortune() {
  if (!fortuneBook.value) return
  // 保存图书引用，打开借阅弹窗让用户选择日期
  const book = fortuneBook.value
  fortuneBorrowing.value = true
  handleBorrow(book)
  // 重置占卜状态
  fortuneRevealed.value = false
  fortuneBook.value = null
  fortuneBorrowing.value = false
}

// 穿搭推荐
async function handleMoodPick(mood) {
  try {
    const res = await recommendByColor(mood.color)
    moodBook.value = res.data
    ElMessage.success(`根据你的${mood.label}心情，推荐《${res.data.title}》`)
  } catch (e) { /* handled */ }
}

async function handleWeather() {
  try {
    const res = await weatherRecommend('')
    weatherBook.value = res.data.book
    const w = res.data.weather
    weatherIcon.value = weatherMap[w]?.icon || '☀️'
    weatherDesc.value = weatherMap[w]?.desc || '点击获取今日天气荐书'
    ElMessage.success(`今日天气：${w}，为你推荐《${res.data.book.title}》`)
  } catch (e) { /* handled */ }
}

async function fetchAtmosphere() {
  try {
    const res = await getTodayStats()
    const data = res.data
    todayBorrowCount.value = data.todayBorrowCount || 0
    atmosphere.value = data.atmosphere || 'quiet'
    const map = {
      hot: { emoji: '🔥', msg: data.message },
      warm: { emoji: '☀️', msg: data.message },
      calm: { emoji: '🍃', msg: data.message },
      quiet: { emoji: '🌙', msg: data.message },
    }
    atmosphereEmoji.value = map[atmosphere.value]?.emoji || '🌙'
    atmosphereMsg.value = map[atmosphere.value]?.msg || data.message
  } catch (e) { /* handled */ }
}

async function fetchPartner() {
  try {
    const res = await getTodayPartner()
    partner.value = res.data
  } catch (e) { partner.value = null }
}

async function handleCoverInterpret(book) {
  interpretationBook.value = book
  interpretationText.value = ''
  try {
    const res = await getCoverInterpretation(book.id)
    interpretationText.value = res.data?.interpretation || '这本书看起来很有趣，翻开它，你会发现惊喜！'
  } catch (e) {
    interpretationText.value = '这本书看起来很有趣，翻开它，你会发现惊喜！'
  }
  interpretationVisible.value = true
}

async function handleGreetPartner() {
  if (!partner.value) return
  const partnerName = partner.value.partner?.nickname || partner.value.partner?.username
  const partnerUsername = partner.value.partner?.username
  greetSending.value = true
  try {
    await sendMessage({
      receiver: partnerUsername,
      content: `你好！我是你的今日阅读搭档，我们一起读完了好书，聊聊感想吧！`
    })
    ElMessage.success(`已向 ${partnerName} 发送问候私信！`)
  } catch (e) {
    // handled by interceptor
  } finally {
    greetSending.value = false
  }
}

onMounted(() => {
  fetchBooks()
  fetchRankings()
  fetchAnnouncements()
  fetchCategories()
  fetchQuotes()
  fetchAtmosphere()
  fetchPartner()
  // 每30分钟刷新氛围
  atmosphereTimer.value = setInterval(fetchAtmosphere, 30 * 60 * 1000)

  // 公告轮播
  announceTimer = setInterval(() => {
    if (announcements.value.length > 0) {
      announceIdx.value = (announceIdx.value + 1) % announcements.value.length
    }
  }, 3000)
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.announcement-bar {
  background: #fdf6ec;
  border: 1px solid #faecd8;
  border-radius: 8px;
  padding: 10px 16px;
  overflow: hidden;
}

.announce-scroll {
  display: flex;
  align-items: center;
  gap: 10px;
}

.announce-text-wrap {
  flex: 1;
  height: 24px;
  overflow: hidden;
  position: relative;
}

.announce-text {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  line-height: 24px;
  font-size: 14px;
  color: #e6a23c;
  opacity: 0;
  transition: opacity 0.5s;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.announce-text.active {
  opacity: 1;
}

.carousel {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.banner-slide {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 80px;
  position: relative;
  overflow: hidden;
}

.banner-text {
  z-index: 2;
  max-width: 50%;
}

.banner-text h2 {
  font-size: 32px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 12px;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.banner-text p {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.15);
}

.banner-img {
  height: 220px;
  border-radius: 12px;
  object-fit: cover;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
  z-index: 2;
}

.main-content {
  display: flex;
  gap: 20px;
}

.content-left {
  flex: 1;
  min-width: 0;
}

.content-right {
  width: 300px;
  flex-shrink: 0;
}

.section {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(255,255,255,0.6);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
  flex-wrap: wrap;
  gap: 12px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.section-search {
  display: flex;
  gap: 10px;
  align-items: center;
}

.category-select {
  width: 140px;
}

.search-input {
  width: 220px;
}

.book-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  min-height: 200px;
}

.book-card {
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(255,255,255,0.5);
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
}

.book-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.12);
  border-color: rgba(167, 139, 250, 0.3);
}

.book-cover {
  height: 140px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.cover-char {
  font-size: 48px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.85);
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.cover-badge {
  position: absolute;
  top: 8px;
  right: 8px;
}

.book-info {
  padding: 14px 16px;
}

.book-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 10px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.book-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
}

.rating-meta {
  display: flex;
  align-items: center;
  gap: 4px;
}

.review-count {
  font-size: 12px;
  color: #c0c4cc;
}

.book-meta .stock {
  font-size: 12px;
  color: #67c23a;
  font-weight: 500;
}

.sale-stock {
  font-size: 12px;
  color: #e6a23c;
  margin-left: 8px;
}

.book-price {
  display: flex;
  align-items: center;
  gap: 6px;
}

.price-current {
  color: #f56c6c;
  font-weight: 700;
  font-size: 16px;
}

.price-original {
  text-decoration: line-through;
  color: #c0c4cc;
  font-size: 12px;
}

.book-actions {
  margin-top: 10px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.ranking-card {
  border-radius: 12px;
  position: sticky;
  top: 20px;
}

.ranking-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
}

.ranking-empty {
  text-align: center;
  padding: 20px;
  color: #c0c4cc;
}

.ranking-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
}

.ranking-item:last-child {
  border-bottom: none;
}

.rank-num {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: #909399;
  flex-shrink: 0;
}

.rank-num.top3 {
  background: linear-gradient(135deg, #f6d365, #fda085);
  color: #fff;
}

.rank-info {
  flex: 1;
  min-width: 0;
}

.rank-title {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rank-meta {
  display: block;
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 2px;
}

.rank-count {
  font-size: 13px;
  font-weight: 600;
  color: #409eff;
  white-space: nowrap;
}

.detail-wrap {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-cover {
  width: 100%;
  height: 180px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.detail-cover-char {
  font-size: 72px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.8);
  text-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
}

.review-section {
  margin-top: 8px;
}

.add-review {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.my-review {
  margin-bottom: 16px;
}

.my-review p {
  margin: 4px 0 0;
  font-size: 13px;
  color: #606266;
}

.review-list {
  max-height: 300px;
  overflow-y: auto;
}

.review-item {
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.review-item:last-child {
  border-bottom: none;
}

.review-user {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.review-username {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.review-comment {
  font-size: 13px;
  color: #606266;
  margin: 6px 0;
  line-height: 1.5;
}

.review-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 4px 0;
}

.review-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.review-likes {
  font-size: 12px;
  color: #909399;
}

.review-time {
  font-size: 12px;
  color: #c0c4cc;
}

.borrow-book-header {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.borrow-form {
  max-height: 60vh;
  overflow-y: auto;
  padding-right: 8px;
}

.borrow-book-img-placeholder {
  width: 80px;
  height: 110px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.borrow-cover-char {
  font-size: 36px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.85);
}

.borrow-book-meta h4 {
  margin: 0 0 8px;
  font-size: 16px;
  color: #303133;
}

.borrow-book-meta p {
  margin: 0 0 4px;
  font-size: 13px;
  color: #909399;
}

/* 盲盒样式 */
.blind-box-container {
  min-height: 300px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.blind-box-animation {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
}

.gift-box {
  width: 120px;
  height: 120px;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.gift-box.shaking {
  animation: shake 0.5s infinite;
}

@keyframes shake {
  0%, 100% { transform: rotate(0deg); }
  25% { transform: rotate(-8deg); }
  75% { transform: rotate(8deg); }
}

.gift-box-lid {
  width: 130px;
  height: 30px;
  background: linear-gradient(135deg, #f6d365, #fda085);
  border-radius: 8px 8px 0 0;
  position: relative;
  top: 2px;
  z-index: 2;
}

.gift-box-body {
  width: 120px;
  height: 90px;
  background: linear-gradient(135deg, #f093fb, #f5576c);
  border-radius: 0 0 12px 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px rgba(240, 147, 251, 0.3);
}

.question-mark {
  font-size: 48px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.9);
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.blind-box-text {
  font-size: 16px;
  color: #909399;
  text-align: center;
}

.blind-box-result {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 8px;
}

.result-book-cover {
  width: 160px;
  height: 220px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  margin-bottom: 12px;
}

.result-cover-char {
  font-size: 64px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.85);
  text-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
}

.blind-box-result h3 {
  font-size: 20px;
  color: #303133;
  margin: 0;
}

.result-author {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.result-desc {
  font-size: 13px;
  color: #606266;
  max-width: 400px;
  line-height: 1.6;
  margin: 4px 0 0;
}

/* 弹幕书摘墙 */
.barrage-wall {
  background: linear-gradient(135deg, #f8f9ff 0%, #e8f0fe 100%);
  border-radius: 12px;
  padding: 14px 20px;
  overflow: hidden;
  border: 1px solid #e0e8ff;
}

.barrage-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.barrage-track {
  height: 36px;
  overflow: hidden;
  position: relative;
}

.barrage-scroll {
  display: flex;
  gap: 32px;
  animation: barrageScroll 30s linear infinite;
  white-space: nowrap;
}

.barrage-item {
  font-size: 13px;
  color: #606266;
  flex-shrink: 0;
  padding: 4px 12px;
  background: rgba(255,255,255,0.7);
  border-radius: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}

.barrage-item small {
  color: #909399;
  margin-left: 6px;
}

.barrage-empty {
  text-align: center;
  color: #c0c4cc;
  font-size: 13px;
  padding: 8px;
}

.quote-input-row {
  display: flex;
  gap: 10px;
  margin-top: 10px;
}

@keyframes barrageScroll {
  0% { transform: translateX(0); }
  100% { transform: translateX(-50%); }
}

/* 趣味功能区 */
.fun-zone {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 16px;
}

.fun-card {
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  padding: 28px;
  text-align: center;
  cursor: pointer;
  box-shadow: 0 4px 20px rgba(0,0,0,0.05);
  border: 1px solid rgba(255,255,255,0.5);
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  min-height: 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.fun-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 36px rgba(0,0,0,0.1);
  border-color: rgba(167, 139, 250, 0.3);
}

.fun-icon {
  font-size: 40px;
  margin-bottom: 12px;
}

.fun-card h3 {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 6px;
}

.fun-card p {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

/* 占卜翻牌 */
.fortune-card {
  perspective: 600px;
  min-height: 240px;
}

.fun-card-inner {
  width: 100%;
  transition: transform 0.8s;
  transform-style: preserve-3d;
}

.fun-card-inner.revealed .fun-card-front {
  display: none;
}

.fun-card.flipping .fun-card-inner {
  animation: flipShake 0.8s ease-in-out;
}

@keyframes flipShake {
  0%, 100% { transform: rotateY(0); }
  50% { transform: rotateY(180deg); }
}

.fun-card-front {
  padding: 20px;
}

.fun-card-back {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  animation: fadeIn 0.5s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: scale(0.9); }
  to { opacity: 1; transform: scale(1); }
}

.fortune-book-cover {
  width: 80px;
  height: 110px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  font-weight: 700;
  color: rgba(255,255,255,0.85);
  text-shadow: 0 2px 8px rgba(0,0,0,0.15);
  box-shadow: 0 4px 16px rgba(0,0,0,0.15);
}

.fortune-card h4 {
  font-size: 16px;
  color: #303133;
  margin: 0;
}

.fortune-text {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  max-width: 280px;
}

/* 穿搭推荐 */
.mood-picker {
  display: flex;
  gap: 10px;
  margin: 12px 0;
}

.mood-dot {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  cursor: pointer;
  transition: transform 0.2s;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
}

.mood-dot:hover {
  transform: scale(1.3);
}

.mood-result {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  animation: fadeIn 0.4s ease;
}

.mood-book-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

/* 天气荐书 */
.weather-card {
  background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%);
}

.weather-result {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  animation: fadeIn 0.4s ease;
}

.weather-book-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

/* 时空胶囊 */
.capsule-section {
  background: linear-gradient(135deg, #fef9e7 0%, #fdebd0 100%);
  border-radius: 12px;
  padding: 16px;
  margin-top: 12px;
  border: 1px dashed #f0c75e;
  text-align: center;
}

.capsule-label {
  font-size: 14px;
  font-weight: 600;
  color: #b8860b;
  margin-bottom: 8px;
}

.capsule-content {
  font-size: 14px;
  color: #5d4037;
  font-style: italic;
  line-height: 1.6;
  margin-bottom: 6px;
}

.capsule-hint {
  font-size: 11px;
  color: #a1887f;
}

/* 今日书架氛围 */
.atmosphere-bar {
  border-radius: 12px;
  padding: 14px 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 500;
  transition: all 0.8s ease;
}

.atmosphere-bar.atmosphere-hot {
  background: linear-gradient(135deg, #fff5f5 0%, #ffe0e0 100%);
  color: #c0392b;
  border: 1px solid #f5c6cb;
}

.atmosphere-bar.atmosphere-warm {
  background: linear-gradient(135deg, #fff8e1 0%, #fff3cd 100%);
  color: #e67e22;
  border: 1px solid #ffeeba;
}

.atmosphere-bar.atmosphere-calm {
  background: linear-gradient(135deg, #e8f5e9 0%, #dcedc8 100%);
  color: #388e3c;
  border: 1px solid #c8e6c9;
}

.atmosphere-bar.atmosphere-quiet {
  background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
  color: #1565c0;
  border: 1px solid #b3e5fc;
}

.atmosphere-emoji { font-size: 22px; }
.atmosphere-text { flex: 1; }
.atmosphere-count { font-size: 13px; opacity: 0.8; }

/* 阅读搭档卡片 */
.partner-card {
  margin-top: 16px;
  cursor: pointer;
}

.partner-envelope {
  background: linear-gradient(135deg, #fef9e7 0%, #fef3c7 100%);
  border: 2px solid #d4a574;
  border-radius: 16px;
  padding: 24px;
  position: relative;
  transition: all 0.5s ease;
  box-shadow: 0 4px 16px rgba(180, 120, 60, 0.15);
}

.partner-envelope.opened {
  background: linear-gradient(135deg, #fff 0%, #fef9e7 100%);
  border-color: #e6a23c;
}

.partner-seal {
  position: absolute;
  top: -15px;
  right: 20px;
  font-size: 32px;
  animation: sealPulse 2s ease-in-out infinite;
  filter: drop-shadow(0 2px 4px rgba(0,0,0,0.2));
}

@keyframes sealPulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.15); }
}

.partner-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.partner-title {
  font-size: 16px;
  font-weight: 600;
  color: #8b6914;
}

.partner-date {
  font-size: 12px;
  color: #b8a06e;
}

.partner-body {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.partner-avatar {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  font-size: 20px;
  font-weight: 700;
}

.partner-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.partner-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.partner-common-books {
  font-size: 13px;
  color: #909399;
}

.partner-greeting {
  text-align: center;
  animation: fadeIn 0.5s ease;
}

.greeting-text {
  font-size: 14px;
  color: #606266;
  font-style: italic;
  margin-bottom: 12px;
}

/* AI解读 */
.interpretation-dialog {
  text-align: center;
  padding: 10px 0;
}

.interpretation-cover {
  width: 100px;
  height: 140px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  box-shadow: 0 6px 20px rgba(0,0,0,0.15);
}

.interpretation-char {
  font-size: 42px;
  font-weight: 700;
  color: rgba(255,255,255,0.85);
  text-shadow: 0 2px 8px rgba(0,0,0,0.15);
}

.interpretation-title {
  font-size: 18px;
  color: #303133;
  margin: 0 0 4px;
}

.interpretation-author {
  font-size: 13px;
  color: #909399;
  margin: 0 0 16px;
}

.interpretation-bubble {
  background: #f0f7ff;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 16px;
  position: relative;
  border: 1px solid #d0e0ff;
}

.interpretation-bubble p {
  font-size: 15px;
  color: #2c3e50;
  line-height: 1.7;
  margin: 0;
  font-style: italic;
}

.interpretation-bubble::before {
  content: '';
  position: absolute;
  top: -10px;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 0;
  border-left: 10px solid transparent;
  border-right: 10px solid transparent;
  border-bottom: 10px solid #f0f7ff;
}

/* 封面hover提示 */
.cover-hover-hint {
  position: absolute;
  bottom: 8px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0,0,0,0.7);
  color: #fff;
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 10px;
  opacity: 0;
  transition: opacity 0.3s;
  white-space: nowrap;
  pointer-events: none;
}

.book-cover:hover .cover-hover-hint {
  opacity: 1;
}

@media (max-width: 1200px) {
  .main-content {
    flex-direction: column;
  }
  .content-right {
    width: 100%;
  }
  .book-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 900px) {
  .book-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .banner-slide {
    padding: 0 40px;
  }
  .banner-text h2 {
    font-size: 24px;
  }
}

@media (max-width: 600px) {
  .book-grid {
    grid-template-columns: 1fr;
  }
}

/* 详情弹窗价格 */
.detail-price {
  display: flex;
  align-items: center;
  gap: 8px;
}
.detail-price .price-current {
  font-size: 18px;
  font-weight: 700;
  color: #f56c6c;
}
.detail-price .price-original {
  text-decoration: line-through;
  color: #c0c4cc;
  font-size: 13px;
}

.buy-success-body { text-align: center; padding: 10px 0; }
.buy-success-icon { font-size: 56px; margin-bottom: 16px; }
.buy-success-text { font-size: 16px; color: #303133; margin-bottom: 8px; font-weight: 500; }
.buy-success-hint { font-size: 14px; color: #909399; }
</style>