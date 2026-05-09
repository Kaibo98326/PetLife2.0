<template>
  <div class="order-history-container py-5">
    <!-- 改用 container-fluid 並增加左右內距 (px-5)，達成全寬度感 -->
    <div class="container-fluid px-lg-5 px-md-4">
      <!-- 標題區：保留原本的裝飾與漸層 -->
      <div class="header-section mb-5">
        <div class="title-group">
          <div class="title-wrapper position-relative">
            <!-- 原本的可愛裝飾點點 -->
            <div class="title-decoration-dots"></div>
            <h2 class="title">
              <!-- 原本的搖擺圖示（加入 icon-pulse 類別） -->
              <div class="icon-pulse">
                <i class="fas fa-paw"></i>
              </div>
              我的訂單紀錄
            </h2>
          </div>
          <div class="subtitle-wrapper">
            <p class="subtitle text-muted">管理您的所有消費紀錄與預約狀態</p>
          </div>
        </div>
      </div>

      <!-- 導覽按鈕區：維持原本的膠囊風格，但確保它們能橫向伸展 -->
      <div class="nav-wrapper d-flex gap-3 mb-4">
        <router-link to="/orderhistory" class="custom-nav-btn" exact-active-class="active">
          <i class="fas fa-shopping-bag me-1"></i> 購買紀錄
        </router-link>
        <router-link to="/orderhistory/prettyorders" class="custom-nav-btn" active-class="active">
          <i class="fas fa-cut me-1"></i> 寵物美容
        </router-link>
        <router-link to="/orderhistory/stayorders" class="custom-nav-btn" active-class="active">
          <i class="fas fa-hotel me-1"></i> 寵物住宿
        </router-link>
      </div>

      <!-- 實際顯示內容區塊：加大 Padding 並設定最小高度 -->
      <div class="content-card shadow-sm">
        <div class="child-route-content">
          <!-- 子路由內容 -->
          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <!-- 加上 :key 確保組件完全重新導航 -->
              <component :is="Component" :key="$route.fullPath" />
            </transition>
          </router-view>
        </div>

        <!-- 返回按鈕區 -->
        <div class="bottom-action-area">
          <router-link to="/member/center" class="back-btn-center">
            <i class="fas fa-reply me-2"></i>返回會員中心
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.order-history-container {
  background-color: #fdfbf7;
  min-height: 100vh; /* 確保全頁背景色一致 */
}

/* 標題與裝飾 */
.title-wrapper {
  display: flex;
  align-items: center;
  margin-left: 20px; /* 替點點留空間 */
}

.title-decoration-dots {
  position: absolute;
  left: -25px;
  width: 8px;
  height: 8px;
  background-color: #f39c12;
  border-radius: 50%;
  box-shadow:
    0 15px 0 #f8c291,
    0 30px 0 #ff793f;
}

.title {
  color: #2c3e50;
  font-weight: 850;
  font-size: clamp(32px, 4vw, 50px); /* 響應式字體 */
  margin: 0;
  display: flex;
  align-items: center;
  background: linear-gradient(45deg, #2c3e50, #e67e22);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

/* 原本的搖擺動畫圖示 */
.icon-pulse {
  background: #ffeaa7;
  -webkit-text-fill-color: #d35400; /* 修復漸層文字下的圖示顏色 */
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  margin-right: 18px;
  font-size: 1.8rem;
  box-shadow: 0 4px 12px rgba(243, 156, 18, 0.2);
  animation: wiggle 3s infinite ease-in-out;
}

@keyframes wiggle {
  0%,
  100% {
    transform: rotate(-8deg);
  }
  50% {
    transform: rotate(8deg);
  }
}

.subtitle {
  font-size: 20px;
  font-weight: 500;
  margin-left: 80px; /* 對齊標題文字 */
}

/* 導覽按鈕：維持原本的寬大膠囊感 */
.custom-nav-btn {
  text-decoration: none;
  padding: 12px 40px;
  border-radius: 50px;
  border: 2px solid #f39c12;
  background-color: white;
  color: #f39c12;
  font-weight: 700;
  font-size: 20px;
  transition: all 0.3s;
}

.active {
  background-color: #f39c12 !important;
  color: white !important;
  box-shadow: 0 4px 12px rgba(243, 156, 18, 0.3);
}

/* 內容卡片：寬版核心 */
.content-card {
  background: white;
  border-radius: 25px;
  padding: 40px;
  min-height: 60vh;
  width: 100%;
  display: flex;
  flex-direction: column;
}

.child-route-content {
  flex-grow: 1;
  width: 100%;
  /* 給一個最小高度，防止組件切換瞬間高度歸零 */
  min-height: 400px;
  position: relative;
}

/* 返回按鈕：維持原本風格 */
.bottom-action-area {
  display: flex;
  justify-content: center;
  margin-top: 50px;
  padding-bottom: 20px;
}

.back-btn-center {
  text-decoration: none;
  color: #e67e22;
  font-size: 22px;
  font-weight: 700;
  padding: 12px 50px;
  border-radius: 50px;
  background: white;
  border: 2px solid #ffeaa7;
  transition: all 0.3s ease;
}

.back-btn-center:hover {
  background-color: #ffeaa7;
  color: #d35400;
  transform: translateY(-3px);
  box-shadow: 0 6px 15px rgba(230, 126, 34, 0.2);
}

/* 過渡動畫 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
