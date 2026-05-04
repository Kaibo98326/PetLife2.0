<script setup>
import { ref } from 'vue'
// 輪播圖片匯入
import ad01 from '@/assets/images/ad01.jpg'
import ad02 from '@/assets/images/ad02.jpg'
import ad03 from '@/assets/images/ad03.jpg'
import ad04 from '@/assets/images/ad04.jpg'
import ad05 from '@/assets/images/ad05.jpg'
import ad06 from '@/assets/images/ad06.jpg'
// 加入購物車按鈕要用的套件
import axios from '@/axios' 
import { useUserStore } from '@/stores/user' 
import Swal from 'sweetalert2' 

const carouselImages = ref([
  { src: ad01, alt: '廣告輪播01' },
  { src: ad02, alt: '廣告輪播02' },
  { src: ad03, alt: '廣告輪播03' },
  { src: ad04, alt: '廣告輪播04' },
  { src: ad05, alt: '廣告輪播05' },
  { src: ad06, alt: '廣告輪播06' }
])

// 模擬商品資料 (之後從 API 拿)
const products = ref([
  { id: 1, name: '貓咪飼料', price: 500, image: '/images/products/catfood.jpg', category: '飼料' },
  { id: 2, name: '狗狗玩具', price: 300, image: '/images/products/dogtoy.jpg', category: '玩具' }
])

// 加入購物車按鈕函式
const userStore = useUserStore(); 

const addToCart = async (p) => {
  console.log("👉 點擊的商品物件:", p);
  console.log("👉 目前 userStore 中的 memberId:", userStore.memberId);

  const memberId = userStore.memberId; 

  if (!memberId) {
    console.warn("❌ 失敗：沒有 memberId，使用者可能未登入或 Store 尚未初始化");
    Swal.fire({ icon: 'info', title: '請先登入' });
    return;
  }

  try {
    const cartItem = {
      productId: p.id,
      quantity: 1
    };
    console.log("🚀 準備發送 POST 到:", `/cart/add/${memberId}`, "資料:", cartItem);

    // 注意：這裡根據你的 axios baseURL 修改路徑
    const res = await axios.post(`/cart/add/${memberId}`, cartItem);
    
    console.log("✅ 後端回傳:", res.data);
    if (res.data === 'success') {
      Swal.fire({ icon: 'success', title: '已加入購物車' });
    }
  } catch (error) {
    console.error("🔥 API 請求失敗:", error.response || error);
    Swal.fire('錯誤', '加入購物車失敗', 'error');
  }
}

</script>

<template>
  <div class="shop-content">
    <div class="container d-flex mt-4">
      <aside class="category-sidebar shadow-sm me-3">
        <div class="sidebar-title"><i class="fas fa-bars me-2"></i>毛孩商品總覽</div>
        <nav class="sidebar-nav">
          <router-link to="/products" class="category-item">全部商品</router-link>
          <router-link to="/products/cat" class="category-item">貓貓專區</router-link>
          <router-link to="/products/dog" class="category-item">狗狗專區</router-link>
        </nav>
      </aside>

      <div id="shopCarousel" class="carousel slide shadow-sm rounded-3 overflow-hidden flex-grow-1" data-bs-ride="carousel">
        <div class="carousel-indicators">
          <button v-for="(img, index) in carouselImages" :key="index" type="button"
            data-bs-target="#shopCarousel" :data-bs-slide-to="index" :class="{ active: index === 0 }"></button>
        </div>
        <div class="carousel-inner">
          <div v-for="(img, index) in carouselImages" :key="index" class="carousel-item"
            :class="{ active: index === 0 }" data-bs-interval="2000">
            <img :src="img.src" :alt="img.alt" class="d-block w-100 img-fluid" style="object-fit: cover; max-height: 500px;">
          </div>
        </div>
        <button class="carousel-control-prev" type="button" data-bs-target="#shopCarousel" data-bs-slide="prev">
          <span class="carousel-control-prev-icon"></span>
        </button>
        <button class="carousel-control-next" type="button" data-bs-target="#shopCarousel" data-bs-slide="next">
          <span class="carousel-control-next-icon"></span>
        </button>
      </div>
    </div>

    <section class="container product-section py-5">
      <h4 class="section-title mb-4">精選好物</h4>
      <div class="row row-cols-2 row-cols-md-4 g-4">
        <div v-for="p in products" :key="p.id" class="col">
          <article class="product-card shadow-sm">
            <div class="product-category-badge">{{ p.category }}</div>
            <router-link :to="`/product/${p.id}`" class="text-decoration-none">
              <div class="product-img-wrapper">
                <img :src="p.image" :alt="p.name" />
              </div>
              <div class="product-info">
                <h6 class="product-name">{{ p.name }}</h6>
              </div>
            </router-link>
            <div class="product-footer">
              <span class="product-price">$ {{ p.price }}</span>
              <!-- <button type="heart" class=""> 愛心丟這裡 </button> -->
              <!-- 檢查這裡的 addToCart 拼字是否跟 script 裡一模一樣 -->
              <button type="button" class="btn add-to-cart-btn" @click="addToCart(p)">
              <i class="fas fa-shopping-basket"></i></button>
            </div>
          </article>
        </div>
      </div>
    </section>
  </div>
</template>