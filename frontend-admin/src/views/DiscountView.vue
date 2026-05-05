<script setup>
import { ref, onMounted, computed } from 'vue';
import axios from 'axios';
// 確保你有引入 bootstrap，以便用 JavaScript 控制 Modal
import * as bootstrap from 'bootstrap';

// ==================== 1. 狀態管理 (State) ====================
const discounts = ref([]); // 存放從後端抓回來的活動清單
const loading = ref(false); // 頁面載入狀態
const isSaving = ref(false); // 按鈕儲存中的旋轉動畫狀態

// 表單綁定的資料模型 (對應你原本的欄位，已移除 is_member)
const formData = ref({
    id: null,
    name: '',
    status: 'active', // 預設啟用
    type: '', // 1: 打折, 2: 折現
    startDate: '',
    endDate: '',
    val: null,
    min: 0,
    desc: ''
});

// 記錄錯誤欄位，用來精準觸發紅框 (取代原本的 DOM 查詢)
const errors = ref({});

// 這裡設定你的 Spring Boot 後端 API 基礎網址
const API_BASE_URL = 'http://localhost:8080/api/discounts'; 

// ==================== 2. API 與資料互動 (Methods) ====================

// 取得所有活動資料 (取代原本的 fetchDiscountList)
const fetchDiscounts = async () => {
    loading.value = true;
    try {
        const response = await axios.get(API_BASE_URL);
        // 假設後端回傳的是陣列，直接賦值給 discounts
        discounts.value = response.data;
    } catch (error) {
        console.error("無法取得活動資料:", error);
    } finally {
        loading.value = false;
    }
};

// 儲存活動 (新增或修改)
const saveActivity = async () => {
    // 1. 清空舊的錯誤狀態
    errors.value = {};
    let isValid = true;

    // 2. 精準防呆檢查 (沒填的就標記為 true，畫面會自動加上紅框)
    if (!formData.value.name) { errors.value.name = true; isValid = false; }
    if (!formData.value.type) { errors.value.type = true; isValid = false; }
    if (!formData.value.startDate) { errors.value.startDate = true; isValid = false; }
    if (!formData.value.endDate) { errors.value.endDate = true; isValid = false; }
    if (!formData.value.desc) { errors.value.desc = true; isValid = false; }
    if (formData.value.val === null || formData.value.val === '') { errors.value.val = true; isValid = false; }

    if (!isValid) {
        alert("請填寫所有必填欄位！");
        return; 
    }

    isSaving.value = true;

    try {
        // 準備送給後端的 payload (數學轉換：如果是打折，把 85 轉成 0.85)
        let finalValue = formData.value.val;
        if (formData.value.type === '1') {
            finalValue = finalValue / 100;
        }

        const payload = {
            discountName: formData.value.name,
            status: formData.value.status,
            startDate: formData.value.startDate,
            endDate: formData.value.endDate,
            discountDescription: formData.value.desc,
            discountValue: finalValue,
            minimumPurchaseAmount: formData.value.min,
            discountType: { discountTypeId: parseInt(formData.value.type) }
        };

        // 判斷是新增 (POST) 還是修改 (PUT)
        if (formData.value.id) {
            await axios.put(`${API_BASE_URL}/${formData.value.id}`, payload);
            alert("活動修改成功！");
        } else {
            await axios.post(API_BASE_URL, payload);
            alert("活動新增成功！");
        }

        // 關閉 Modal 並重新抓取資料
        const modalEl = document.getElementById('formModal');
        const modalInstance = bootstrap.Modal.getInstance(modalEl);
        if (modalInstance) modalInstance.hide();
        
        fetchDiscounts();

    } catch (error) {
        console.error("儲存失敗:", error);
        alert("處理失敗，請檢查網路或後端伺服器！");
    } finally {
        isSaving.value = false;
    }
};

// 刪除活動
const deleteActivity = async (id, status) => {
    if (status === 'active') {
        alert("無法刪除！\n\n系統禁止刪除「進行中」活動。\n\n👉 如需停止，請將狀態改為「已停用」。");
        return;
    }

    if (!confirm('確定要刪除這筆活動嗎？')) return;

    try {
        await axios.delete(`${API_BASE_URL}/${id}`);
        alert('活動已刪除！');
        fetchDiscounts();
    } catch (error) {
        alert('刪除失敗');
    }
};

// ==================== 3. 畫面互動控制 (UI Logic) ====================

// 打開新增 Modal，重置所有欄位
const openAddModal = () => {
    formData.value = {
        id: null, name: '', status: 'active', type: '', 
        startDate: '', endDate: '', val: null, min: 0, desc: ''
    };
    errors.value = {}; // 清除紅框
};

// 打開修改 Modal，把資料填入
const openEditModal = (item) => {
    errors.value = {}; // 清除紅框
    
    // 處理打折的數學轉換：後端 0.85 -> 前端顯示 85
    let displayValue = item.discountValue;
    if (item.discountType && item.discountType.discountTypeId === 1) {
        displayValue = Math.round(item.discountValue * 100);
    }

    formData.value = {
        id: item.discountId,
        name: item.discountName,
        status: item.status,
        type: item.discountType ? item.discountType.discountTypeId.toString() : '',
        startDate: item.startDate,
        endDate: item.endDate,
        val: displayValue,
        min: item.minimumPurchaseAmount,
        desc: item.discountDescription
    };
};

// 計算日期最小值的綁定
const todayDate = computed(() => {
    return new Date().toISOString().split('T')[0];
});

// 解析狀態標籤 (Vue 的寫法：回傳物件讓 Template 綁定 class)
const getStatusBadge = (status, startStr, endStr) => {
    if (status === 'inactive') return { text: '🔴 已停用', class: 'bg-danger' };
    
    const now = new Date();
    const start = new Date(startStr.replace(/-/g, '/'));
    start.setHours(0, 0, 0, 0);
    const end = new Date(endStr.replace(/-/g, '/'));
    end.setHours(23, 59, 59, 999);
    
    if (now < start) {
        const hoursDiff = (start.getTime() - now.getTime()) / (1000 * 60 * 60);
        return hoursDiff <= 24 
            ? { text: '🟡 即將開始', class: 'bg-warning text-dark' }
            : { text: '🔵 尚未開始', class: 'bg-info text-dark' };
    }
    if (now >= start && now <= end) {
        return { text: '🟢 進行中', class: 'bg-success' };
    }
    return { text: '⚪ 已結束', class: 'bg-secondary' };
};

// 網頁載入時立刻抓資料
onMounted(() => {
    fetchDiscounts();
});
</script>

<template>
    <div class="container-fluid py-3">

        <div id="listView">
            <div class="w-100 mb-2">
                <!-- 頂部搜尋與操作區 -->
                <div class="d-flex justify-content-between align-items-center mb-3 w-100">
                    <div class="d-flex align-items-center gap-2">
                        <!-- 搜尋框 -->
                        <input type="text" class="form-control" placeholder="🔍 搜尋活動名稱..." style="width: 250px;">

                        <!-- 進階篩選下拉選單 -->
                        <div class="dropdown">
                            <button class="btn btn-outline-secondary dropdown-toggle position-relative" type="button" data-bs-toggle="dropdown" data-bs-auto-close="outside">
                                ⚙️ 進階篩選條件
                                <span class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger d-none" style="font-size: 0.7rem;">
                                    已套用
                                </span>
                            </button>
                            
                            <div class="dropdown-menu shadow-lg p-4 rounded-3 mt-2" style="min-width: 450px;">
                                <div class="d-flex justify-content-between align-items-center border-bottom pb-2 mb-3">
                                    <h6 class="mb-0 fw-bold">⚙️ 進階篩選條件</h6>
                                </div>
                                <div class="mb-3">
                                    <div class="text-muted small mb-2">活動狀態 (Status)</div>
                                    <div class="d-flex flex-wrap gap-2">
                                        <button class="btn btn-outline-secondary btn-sm active">全部</button>
                                        <button class="btn btn-outline-secondary btn-sm">🔵 尚未開始</button>
                                        <button class="btn btn-outline-secondary btn-sm">🟡 即將開始</button>
                                        <button class="btn btn-outline-secondary btn-sm">🟢 進行中</button>
                                        <button class="btn btn-outline-secondary btn-sm">⚪ 已結束</button>
                                        <button class="btn btn-outline-secondary btn-sm">🔴 已停用</button>
                                    </div>
                                </div>
                                <div class="mb-3">
                                    <div class="text-muted small mb-2">活動類型 (Type)</div>
                                    <div class="d-flex flex-wrap gap-2">
                                        <button class="btn btn-outline-secondary btn-sm active">全部</button>
                                        <button class="btn btn-outline-secondary btn-sm">百分比折扣</button>
                                        <button class="btn btn-outline-secondary btn-sm">滿額折扣</button>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- 新增活動按鈕 (綁定 Vue 方法) -->
                        <button class="btn btn-success fw-bold" data-bs-toggle="modal" data-bs-target="#formModal" @click="openAddModal">
                            ➕ 新增活動
                        </button>
                    </div>

                    <!-- 右側統計數字 -->
                    <div class="d-flex justify-content-end align-items-center gap-4 text-muted">
                        <div><span>總活動數</span> <strong class="text-dark">{{ discounts.length }}</strong></div>
                        <div><span>搜尋結果</span> <strong class="text-dark">-</strong></div>
                    </div>
                </div>

                <!-- 活動列表表格 -->
                <div class="table-responsive bg-white rounded shadow-sm border p-3">
                    <div v-if="loading" class="text-center py-4">
                        <div class="spinner-border text-primary" role="status"></div>
                        <div class="mt-2 text-muted">資料載入中...</div>
                    </div>

                    <table v-else class="table table-hover align-middle">
                        <thead class="table-light">
                            <tr>
                                <th>編號</th>
                                <th>狀態</th>
                                <th>活動名稱</th>
                                <th>折扣類別</th>
                                <th>活動期間</th>
                                <th>操作</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-if="discounts.length === 0">
                                <td colspan="6" class="text-center text-muted py-4">目前尚無活動資料</td>
                            </tr>
                            <tr v-for="(item, index) in discounts" :key="item.discountId">
                                <td>{{ item.discountId }}</td>
                                <td>
                                    <span class="badge rounded-pill" :class="getStatusBadge(item.status, item.startDate, item.endDate).class" style="font-size: 0.9rem; padding: 0.45em 0.85em;">
                                        {{ getStatusBadge(item.status, item.startDate, item.endDate).text }}
                                   </span>
                                </td>
                                <td class="fw-bold">{{ item.discountName }}</td>
                                <td>{{ item.discountType && item.discountType.discountTypeId === 1 ? '百分比折扣' : '滿額折扣' }}</td>
                                <td>{{ item.startDate }} ~ {{ item.endDate }}</td>
                                <td>
                                    <button class="btn btn-sm text-primary fw-bold bg-transparent border-0 px-2" data-bs-toggle="modal" data-bs-target="#formModal" @click="openEditModal(item)">查看/修改</button>
                                    <button class="btn btn-sm text-danger fw-bold bg-transparent border-0 px-2" @click="deleteActivity(item.discountId, item.status)">刪除</button>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <!-- 新增/修改活動的 Modal (含頁籤設計) -->
        <div class="modal fade" id="formModal" tabindex="-1" data-bs-backdrop="static">
            <div class="modal-dialog modal-lg modal-dialog-scrollable">
                <div class="modal-content">
                    <div class="modal-header bg-light">
                        <h5 class="modal-title fw-bold">{{ formData.id ? '✏️ 修改優惠活動' : '✨ 新增優惠活動' }}</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body p-0">
                        
                        <!-- 頁籤導覽列 -->
                        <ul class="nav nav-tabs px-4 pt-3 bg-light" role="tablist">
                            <li class="nav-item">
                                <button class="nav-link active fw-bold" id="rules-tab" data-bs-toggle="tab" data-bs-target="#rules" type="button">📜 活動規則</button>
                            </li>
                            <li class="nav-item">
                                <button class="nav-link fw-bold" id="products-tab" data-bs-toggle="tab" data-bs-target="#products" type="button">📦 適用商品清單</button>
                            </li>
                        </ul>

                        <div class="tab-content p-4">
                            <!-- 頁籤 1：活動規則表單 -->
                            <div class="tab-pane fade show active" id="rules">
                                <form @submit.prevent>
                                    <div class="row mb-3">
                                        <div class="col-md-3">
                                            <label class="form-label text-muted">狀態 <span class="text-danger">*</span></label>
                                            <select class="form-select" v-model="formData.status">
                                                <option value="active">啟用</option>
                                                <option value="inactive">停用</option>
                                            </select>
                                        </div>
                                        <div class="col-md-9">
                                            <label class="form-label text-muted">活動名稱 <span class="text-danger">*</span></label>
                                            <input type="text" class="form-control" v-model.trim="formData.name" :class="{'is-invalid': errors.name}" placeholder="請輸入活動名稱 (最多 100 字)">
                                        </div>
                                    </div>

                                    <div class="mb-3">
                                        <label class="form-label text-muted">折扣類型 <span class="text-danger">*</span></label>
                                        <select class="form-select" v-model="formData.type" :class="{'is-invalid': errors.type}">
                                            <option value="" disabled>請選擇折扣類型</option>
                                            <option value="1">百分比折扣 (打折)</option>
                                            <option value="2">滿額折扣 (折現)</option>
                                        </select>
                                    </div>

                                    <!-- 動態欄位區 (完美對稱排版) -->
                                    <div class="p-3 bg-light rounded mb-3 border" v-if="formData.type">
                                        <div class="row">
                                            <div class="col-md-6">
                                                <label class="form-label text-muted">最低消費金額 (元) <span class="text-danger">*</span></label>
                                                <input type="number" class="form-control" v-model.number="formData.min" min="0" placeholder="無門檻請填 0">
                                            </div>
                                            <div class="col-md-6">
                                                <label class="form-label fw-bold">折扣值 <span class="text-danger">*</span></label>
                                                <div class="input-group">
                                                    <input type="number" class="form-control" v-model.number="formData.val" :class="{'is-invalid': errors.val}"
                                                           :placeholder="formData.type === '1' ? '輸入 85 打8.5折' : '輸入 100 折扣100元'"
                                                           :max="formData.type === '1' ? 99 : null">
                                                    <span class="input-group-text">{{ formData.type === '1' ? '%' : '元' }}</span>
                                                </div>
                                            </div>
                                        </div>
                                    </div>

                                    <div class="row mb-3">
                                        <div class="col-md-6">
                                            <label class="form-label text-muted">開始日期 <span class="text-danger">*</span></label>
                                            <input type="date" class="form-control" v-model="formData.startDate" :class="{'is-invalid': errors.startDate}" :min="todayDate">
                                        </div>
                                        <div class="col-md-6">
                                            <label class="form-label text-muted">結束日期 <span class="text-danger">*</span></label>
                                            <input type="date" class="form-control" v-model="formData.endDate" :class="{'is-invalid': errors.endDate}" :min="formData.startDate">
                                        </div>
                                    </div>

                                    <div class="mb-2">
                                        <label class="form-label text-muted">活動描述 <span class="text-danger">*</span></label>
                                        <textarea class="form-control" rows="3" v-model="formData.desc" :class="{'is-invalid': errors.desc}" maxlength="500"></textarea>
                                        <div class="text-end small text-muted mt-1">
                                            <span>{{ formData.desc.length }}</span> / 500
                                        </div>
                                    </div>
                                </form>
                            </div>

                            <!-- 頁籤 2：商品清單 -->
                            <div class="tab-pane fade" id="products">
                                <div class="alert alert-info border-0 shadow-sm">ℹ️ 未來將在此處新增商品關聯列表 (主商品與加購品)。</div>
                            </div>
                        </div>

                    </div>
                    <div class="modal-footer bg-light">
                        <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">取消</button>
                        <button type="button" class="btn btn-primary px-4" @click="saveActivity" :disabled="isSaving">
                            <span v-if="isSaving" class="spinner-border spinner-border-sm me-1"></span>
                            {{ isSaving ? '處理中...' : '儲存並發布' }}
                        </button>
                    </div>
                </div>
            </div>
        </div>

    </div>
</template>

<style scoped>
/* 將原本 discount.css 裡面的特殊樣式搬過來 */
.btn.bg-transparent:hover {
    background-color: #f8f9fa !important;
    border-radius: 4px;
}
.nav-tabs .nav-link {
    color: #6c757d;
}
.nav-tabs .nav-link.active {
    color: #0d6efd;
    border-bottom: 2px solid #0d6efd;
}
/* 進階篩選按鈕列的自訂樣式 */
.filter-group button.active {
    background-color: #6c757d;
    color: white;
}
</style>