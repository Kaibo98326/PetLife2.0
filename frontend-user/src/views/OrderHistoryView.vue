<template>
  <div class="order-history-container py-5">
    <div class="container">
      <!-- 標題區 -->
      <div class="header-section mb-5">
        <div class="title-group">
          <div class="title-wrapper">
            <div class="title-decoration-dots"></div>

            <h2 class="title"><i class="fas fa-paw">我的訂單紀錄</i></h2>
          </div>
          <div class="subtitle-wrapper">
            <p class="subtitle text-muted">
              <i class="fas fa-paw"></i>管理您的所有消費紀錄與預約狀態
            </p>
          </div>
        </div>
      </div>

      <!-- 導覽按鈕區 -->
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

      <!-- 實際顯示內容區塊 -->
      <div class="content-card shadow-sm">
        <!-- 子路由內容 -->
        <router-view></router-view>

        <!-- 返回按鈕移動到這裡：內容最下方且置中 -->
        <div class="bottom-action-area"></div>
        <router-link to="/member/center" class="back-btn-center">
          <i class="fas fa-reply me-2"></i>返回會員中心
        </router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ... 保留您原本大部分的 CSS ... */

.order-history-container {
  background-color: #fdfbf7;
  min-height: 80vh;
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
  font-size: 50px;
  margin: 0;
  display: flex;
  align-items: center;
  background: linear-gradient(45deg, #2c3e50, #e67e22);
  -webkit-background-clip: text; /*讓文字蓋掉優先背景*/
  -webkit-text-fill-color: transparent;
}

.icon-pulse {
  background: #ffeaa7;
  color: #d35400;
  width: 50px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  margin-right: 18px;
  font-size: 1.5rem;
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
  color: #7f8c8d !important;
  margin-left: 30px;
  bottom: 20px;
}

/* --- 按鈕樣式 --- */
.bottom-action-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
}

.back-btn-center {
  text-decoration: none;
  color: #e67e22;
  font-size: 23px;
  font-weight: 700;
  padding: 12px 40px;
  border-radius: 50px;
  background: white;
  border: 2px solid #ffeaa7;
  transition: all 0.3s ease;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.05);
}

.back-btn-center:hover {
  background-color: #ffeaa7;
  color: #d35400;
  transform: translateY(-3px); /* 向上浮動 */
  box-shadow: 0 6px 15px rgba(230, 126, 34, 0.2);
}

/* 導覽按鈕、卡片 */
.custom-nav-btn {
  text-decoration: none;
  padding: 12px 30px;
  border-radius: 50px;
  border: 2px solid #f39c12;
  background-color: white;
  color: #f39c12;
  font-weight: 700;
  font-size: 22px;
  transition: 0.3s;
}

.active {
  background-color: #f39c12 !important;
  color: white !important;
}

.content-card {
  background: white;
  border-radius: 25px;
  padding: 35px;
  border: none;
  min-height: 400px;
  display: flex;
  flex-direction: column; /* 讓內部垂直排列 */
  justify-content: space-between; /* 內容在頂，按鈕在底 */
}

/* 確保內容會把按鈕往下推 */
.content-card > :first-child {
  flex-grow: 1;
}

/* 動畫 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
