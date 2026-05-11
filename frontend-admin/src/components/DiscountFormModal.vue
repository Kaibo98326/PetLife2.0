<script setup>
import { ref, computed } from 'vue';
import axios from 'axios';
import * as bootstrap from 'bootstrap';

const props = defineProps(['apiBaseUrl', 'discountTypes']);
const emit = defineEmits(['saved']);

const currentStep = ref(1);
const tempScopeType = ref(null);
const isSaving = ref(false);

// 勾選狀態管理
const selectedCategoryIds = ref([]);
const selectedMainProductIds = ref([]);
const selectedAddonProductIds = ref([]);
const selectedAddonCategoryIds = ref([]); // 支援分類級加購

// 搜尋與篩選控制
const searchCategory = ref('');
const showSelectedCategoryOnly = ref(false);
const searchProduct = ref('');
const showSelectedProductOnly = ref(false);

const formData = ref({
    id: null, scopeType: 1, name: '', status: 'active', type: '', 
    startDate: '', endDate: '', desc: '', min: null, val: null, buyQuantity: null, freeQuantity: null 
});

const errors = ref({}); 

// 資料清單
const productsList = ref([]);
const categoriesList = ref([]);

// 抓取真實資料庫資料
const fetchOptions = async () => {
    try {
        // 對接 InnerProductController 的 /list
        const prodRes = await axios.get('http://localhost:8082/api/products/list');
        productsList.value = prodRes.data.productList || [];
        
        // 對接 InnerCategoryController
        const catRes = await axios.get('http://localhost:8082/api/categories');
        categoriesList.value = catRes.data || [];
    } catch (error) {
        console.warn('目前無法取得商品或分類資料，請確認後端 API 是否已準備好。', error);
    }
};

// ✨ 判斷是否為三階段流程：買N送M (3) 或 條件加購 (4)
const isThreeStep = computed(() => {
    return formData.value.type === '3' || formData.value.type === '4';
});

// 日期防呆：取得今日日期
const todayDate = computed(() => {
    const today = new Date();
    const yyyy = today.getFullYear();
    const mm = String(today.getMonth() + 1).padStart(2, '0');
    const dd = String(today.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
});

// 分類表格過濾邏輯
const filteredCategories = computed(() => {
    let res = categoriesList.value;
    if (currentStep.value === 2 && showSelectedCategoryOnly.value) res = res.filter(c => selectedCategoryIds.value.includes(c.categoryId));
    if (currentStep.value === 3 && showSelectedCategoryOnly.value) res = res.filter(c => selectedAddonCategoryIds.value.includes(c.categoryId));
    if (searchCategory.value) res = res.filter(c => c.categoryName.includes(searchCategory.value));
    return res;
});

// 商品表格過濾邏輯
const filteredProducts = computed(() => {
    let res = productsList.value;
    if (currentStep.value === 2 && showSelectedProductOnly.value) res = res.filter(p => selectedMainProductIds.value.includes(p.productId));
    if (currentStep.value === 3 && showSelectedProductOnly.value) res = res.filter(p => selectedAddonProductIds.value.includes(p.productId));
    if (searchProduct.value) res = res.filter(p => p.productName.includes(searchProduct.value));
    return res;
});

// 下一步按鈕文字
const nextButtonText = computed(() => {
    if (currentStep.value === 1) return isThreeStep.value ? '下一步：選擇主商品 ➔' : '下一步：選擇適用清單 ➔';
    if (currentStep.value === 2) return isThreeStep.value ? '下一步：選擇加購/贈送商品 ➔' : '儲存並發布';
    return '儲存並發布';
});

// 檢查是否可以按下一步
const isNextDisabled = computed(() => {
    if (currentStep.value === 2) {
        return formData.value.scopeType === 1 ? selectedCategoryIds.value.length === 0 : selectedMainProductIds.value.length === 0;
    }
    if (currentStep.value === 3) {
        return formData.value.scopeType === 1 ? selectedAddonCategoryIds.value.length === 0 : selectedAddonProductIds.value.length === 0;
    }
    return false;
});

// 處理步驟切換與防呆
const handleNextStep = async () => {
    if (currentStep.value === 1) {
        errors.value = {}; let isValid = true;
        const fields = ['name', 'type', 'startDate', 'endDate', 'desc'];
        fields.forEach(f => { if (!formData.value[f] && formData.value[f] !== 0) { errors.value[f] = true; isValid = false; } });
        if (formData.value.min === null || formData.value.min === '') { errors.value.min = true; isValid = false; }

        const t = formData.value.type;
        if ((t === '1' || t === '2' || t === '4' || t === '5') && (formData.value.val === null || formData.value.val === '')) { errors.value.val = true; isValid = false; }
        if ((t === '3' || t === '4' || t === '5') && (formData.value.buyQuantity === null || formData.value.buyQuantity === '')) { errors.value.buyQuantity = true; isValid = false; }
        if (t === '3' && (formData.value.freeQuantity === null || formData.value.freeQuantity === '')) { errors.value.freeQuantity = true; isValid = false; }

        if (!isValid) return;
        currentStep.value = 2;
    } else if (currentStep.value === 2) {
        if (isThreeStep.value) {
            // 分類買 N 送 M 防呆：自動帶入相同分類
            if (formData.value.scopeType === 1 && formData.value.type === '3') {
                selectedAddonCategoryIds.value = [...selectedCategoryIds.value];
            }
            currentStep.value = 3; 
            searchProduct.value = ''; searchCategory.value = '';
            showSelectedProductOnly.value = false; showSelectedCategoryOnly.value = false;
        } 
        else { await saveActivity(); }
    } else { await saveActivity(); }
};

const handlePrevStep = () => { currentStep.value--; };

// Radio 切換警告與瞬間回彈機制
const handleScopeChange = (type) => {
    const oldType = type === 1 ? 2 : 1;
    const hasSelected = selectedCategoryIds.value.length > 0 || 
                        selectedMainProductIds.value.length > 0 || 
                        selectedAddonProductIds.value.length > 0 ||
                        selectedAddonCategoryIds.value.length > 0;
                        
    if (hasSelected) {
        formData.value.scopeType = oldType; 
        tempScopeType.value = type;
        const el = document.getElementById('warnModal');
        const modal = bootstrap.Modal.getInstance(el) || new bootstrap.Modal(el);
        modal.show();
    }
};

const confirmChangeScope = () => {
    formData.value.scopeType = tempScopeType.value;
    selectedCategoryIds.value = []; selectedMainProductIds.value = []; 
    selectedAddonProductIds.value = []; selectedAddonCategoryIds.value = [];
    const modal = bootstrap.Modal.getInstance(document.getElementById('warnModal'));
    if (modal) modal.hide();
};

// 儲存邏輯
const saveActivity = async () => {
    isSaving.value = true;
    try {
        let finalValue = null;
        let finalBuyQty = null;
        let finalFreeQty = null;
        const typeCode = formData.value.type;

        if (typeCode === '1') { finalValue = formData.value.val / 100; } 
        else if (typeCode === '2') { finalValue = formData.value.val; } 
        else if (typeCode === '3') {
            finalBuyQty = formData.value.buyQuantity;
            finalFreeQty = formData.value.freeQuantity;
        } else if (typeCode === '4' || typeCode === '5') {
            finalBuyQty = formData.value.buyQuantity;
            finalValue = formData.value.val;
        }

        const payload = {
            discount: {
                discountId: formData.value.id,
                discountName: formData.value.name,
                scopeType: formData.value.scopeType,
                status: formData.value.status,
                startDate: formData.value.startDate,
                endDate: formData.value.endDate,
                discountDescription: formData.value.desc,
                minimumPurchaseAmount: formData.value.min || 0,
                discountValue: finalValue,
                buyQuantity: finalBuyQty,
                freeQuantity: finalFreeQty,
                discountType: { discountTypeId: parseInt(formData.value.type) }
            },
            categoryIds: formData.value.scopeType === 1 ? selectedCategoryIds.value : [],
            mainProductIds: formData.value.scopeType === 2 ? selectedMainProductIds.value : [],
            addonProductIds: (formData.value.scopeType === 2 && isThreeStep.value) ? selectedAddonProductIds.value : [],
            addonCategoryIds: (formData.value.scopeType === 1 && isThreeStep.value) ? selectedAddonCategoryIds.value : [] // 支援分類副商品
        };

        if (formData.value.id) await axios.put(`${props.apiBaseUrl}/${formData.value.id}`, payload);
        else await axios.post(`${props.apiBaseUrl}/save`, payload);

        emit('saved');
        bootstrap.Modal.getInstance(document.getElementById('formModal')).hide();
    } catch (error) { 
        alert("儲存失敗，請檢查資料格式：" + (error.response?.data || error.message)); 
    }
    finally { isSaving.value = false; }
};

const showFormModal = () => {
    const el = document.getElementById('formModal');
    const modal = bootstrap.Modal.getInstance(el) || new bootstrap.Modal(el);
    modal.show();
};

defineExpose({
    openAdd() {
        formData.value = { id: null, scopeType: 1, name: '', status: 'active', type: '', startDate: '', endDate: '', desc: '', min: null, val: null, buyQuantity: null, freeQuantity: null };
        currentStep.value = 1; errors.value = {};
        selectedCategoryIds.value = []; selectedMainProductIds.value = []; 
        selectedAddonProductIds.value = []; selectedAddonCategoryIds.value = [];
        searchProduct.value = ''; searchCategory.value = '';
        showSelectedProductOnly.value = false; showSelectedCategoryOnly.value = false;
        fetchOptions(); 
        showFormModal();
    },
    openEdit(item) {
        errors.value = {};
        currentStep.value = 1; 
        let displayValue = item.discountValue;
        if (item.discountType && item.discountType.discountTypeId === 1) {
            displayValue = Math.round(item.discountValue * 100);
        }
        formData.value = {
            id: item.discountId, scopeType: item.scopeType || 1, name: item.discountName,
            status: item.status, type: item.discountType ? item.discountType.discountTypeId.toString() : '',
            startDate: item.startDate, endDate: item.endDate, desc: item.discountDescription,
            min: item.minimumPurchaseAmount, val: displayValue,
            buyQuantity: item.buyQuantity, freeQuantity: item.freeQuantity
        };
        fetchOptions(); 
        showFormModal();
    }
});
</script>

<template>
    <div>
        <div class="modal fade" id="formModal" tabindex="-1" data-bs-backdrop="static">
            <div class="modal-dialog modal-lg modal-dialog-scrollable">
                <div class="modal-content">
                    <div class="modal-header bg-light">
                        <h5 class="modal-title fw-bold">✨ {{ formData.id ? '修改' : '新增' }}優惠活動</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body p-0">
                        <ul class="nav nav-tabs px-4 pt-3 bg-light">
                            <li class="nav-item"><button class="nav-link fw-bold" :class="{active: currentStep===1}" @click="currentStep=1">📜 活動規則</button></li>
                            <li class="nav-item">
                                <button class="nav-link fw-bold" :class="{active: currentStep===2, 'pointer-events-none opacity-50': currentStep < 2}" @click="currentStep=2">
                                    📦 {{ formData.scopeType === 1 ? (isThreeStep ? '選擇主分類' : '指定分類') : (isThreeStep ? '選擇主商品' : '指定單品') }}
                                </button>
                            </li>
                            <li class="nav-item" v-if="isThreeStep">
                                <button class="nav-link fw-bold" :class="{active: currentStep===3, 'pointer-events-none opacity-50': currentStep < 3}" @click="currentStep=3">
                                    🎁 選擇副商品
                                </button>
                            </li>
                        </ul>

                        <div class="tab-content p-4">
                            <div v-show="currentStep === 1">
                                <form @submit.prevent>
                                    <div class="mb-3 p-3 bg-light border rounded">
                                        <label class="form-label fw-bold mb-2">適用範圍 <span class="text-danger">*</span></label>
                                        <div class="d-flex gap-4">
                                            <div class="form-check">
                                                <input class="form-check-input" type="radio" :value="1" v-model="formData.scopeType" @change="handleScopeChange(1)" id="scopeType1">
                                                <label class="form-check-label fw-bold" for="scopeType1">指定分類清單</label>
                                            </div>
                                            <div class="form-check">
                                                <input class="form-check-input" type="radio" :value="2" v-model="formData.scopeType" @change="handleScopeChange(2)" id="scopeType2">
                                                <label class="form-check-label fw-bold" for="scopeType2">指定單品清單</label>
                                            </div>
                                        </div>
                                    </div>

                                    <div class="row mb-3">
                                        <div class="col-md-3">
                                            <label class="form-label text-muted">狀態 <span class="text-danger">*</span></label>
                                            <select class="form-select" v-model="formData.status"><option value="active">啟用</option><option value="inactive">停用</option></select>
                                        </div>
                                        <div class="col-md-9">
                                            <label class="form-label text-muted">活動名稱 <span class="text-danger">*</span></label>
                                            <input type="text" class="form-control" v-model.trim="formData.name" :class="{'is-invalid': errors.name}" placeholder="請輸入活動名稱">
                                            <div v-if="errors.name" class="text-danger small mt-1">此欄位為必填</div>
                                        </div>
                                    </div>

                                    <div class="mb-3">
                                        <label class="form-label text-muted">折扣類型 <span class="text-danger">*</span></label>
                                        <select class="form-select" v-model="formData.type" @change="formData.val=null;formData.buyQuantity=null;formData.freeQuantity=null" :class="{'is-invalid': errors.type}">
                                            <option value="" disabled>請選擇折扣類型</option>
                                            <option v-for="t in props.discountTypes" :key="t.discountTypeId" :value="t.discountTypeId.toString()">{{ t.discountTypeName }}</option>
                                        </select>
                                        <div v-if="errors.type" class="text-danger small mt-1">請選擇折扣類型</div>
                                    </div>

                                    <div class="p-3 bg-light rounded mb-3 border" v-if="formData.type">
                                        <div class="row" v-if="formData.type === '1' || formData.type === '2'">
                                            <div class="col-12">
                                                <label class="form-label fw-bold">折扣值 <span class="text-danger">*</span></label>
                                                <div class="input-group">
                                                    <input type="number" class="form-control" v-model.number="formData.val" :class="{'is-invalid': errors.val}" :placeholder="formData.type === '1' ? '輸入 85 打8.5折' : '輸入折扣金額'">
                                                    <span class="input-group-text">{{ formData.type === '1' ? '%' : '元' }}</span>
                                                </div>
                                            </div>
                                        </div>
                                        <div class="row" v-if="formData.type === '3'">
                                            <div class="col-6"><label class="form-label fw-bold">買 N 件 <span class="text-danger">*</span></label><input type="number" class="form-control" v-model.number="formData.buyQuantity"></div>
                                            <div class="col-6"><label class="form-label fw-bold">送 M 件 <span class="text-danger">*</span></label><input type="number" class="form-control" v-model.number="formData.freeQuantity"></div>
                                        </div>
                                        <div class="row" v-if="formData.type === '4' || formData.type === '5'">
                                            <div class="col-6"><label class="form-label fw-bold">{{ formData.type === '4' ? '主商品需滿 N 件' : '任選 N 件' }} <span class="text-danger">*</span></label><input type="number" class="form-control" v-model.number="formData.buyQuantity"></div>
                                            <div class="col-6"><label class="form-label fw-bold">{{ formData.type === '4' ? '副商品加購價金額' : '組合總價' }} <span class="text-danger">*</span></label><input type="number" class="form-control" v-model.number="formData.val"></div>
                                        </div>
                                    </div>

                                    <div class="row mb-3">
                                        <div class="col-md-4"><label class="form-label text-muted">最低金額門檻 *</label><input type="number" class="form-control" v-model.number="formData.min" placeholder="無門檻填 0"></div>
                                        <div class="col-md-4"><label class="form-label text-muted">開始日期 *</label><input type="date" class="form-control" v-model="formData.startDate" :min="todayDate"></div>
                                        <div class="col-md-4"><label class="form-label text-muted">結束日期 *</label><input type="date" class="form-control" v-model="formData.endDate" :min="formData.startDate || todayDate"></div>
                                    </div>
                                    <div class="mb-2"><label class="form-label text-muted">活動描述 *</label><textarea class="form-control" rows="3" v-model="formData.desc"></textarea></div>
                                </form>
                            </div>

                            <div v-show="currentStep === 2">
                                <div class="d-flex justify-content-between align-items-center bg-light p-3 rounded mb-3 border">
                                    <input v-if="formData.scopeType === 1" type="text" class="form-control form-control-sm" v-model="searchCategory" placeholder="🔍 搜尋名稱..." style="width: 200px;">
                                    <input v-else type="text" class="form-control form-control-sm" v-model="searchProduct" placeholder="🔍 搜尋名稱..." style="width: 200px;">
                                    <div class="d-flex align-items-center gap-4">
                                        <span class="fw-bold text-primary">已勾選：{{ formData.scopeType === 1 ? selectedCategoryIds.length : selectedMainProductIds.length }} 件</span>
                                        
                                        <div class="form-check form-switch mb-0 d-flex align-items-center gap-2">
                                            <template v-if="formData.scopeType === 1">
                                                <input class="form-check-input mt-0" type="checkbox" v-model="showSelectedCategoryOnly" id="showSelCatMain">
                                                <label class="form-check-label small mb-0" for="showSelCatMain">只顯示已勾選</label>
                                            </template>
                                            <template v-else>
                                                <input class="form-check-input mt-0" type="checkbox" v-model="showSelectedProductOnly" id="showSelProdMain">
                                                <label class="form-check-label small mb-0" for="showSelProdMain">只顯示已勾選</label>
                                            </template>
                                        </div>
                                    </div>
                                </div>

                                <h6 v-if="isThreeStep" class="fw-bold text-primary mb-2 ps-1">📦 主商品/主分類</h6>

                                <div class="table-responsive border rounded" style="max-height: 350px;">
                                    <table class="table table-hover table-sm align-middle mb-0">
                                        <thead class="table-light position-sticky top-0">
                                            <tr><th style="width: 60px;">選取</th><th>名稱</th><th v-if="formData.scopeType===2">庫存</th><th v-if="formData.scopeType===2">價格</th></tr>
                                        </thead>
                                        <tbody>
                                            <template v-if="formData.scopeType === 1">
                                                <tr v-for="c in filteredCategories" :key="c.categoryId">
                                                    <td><input type="checkbox" class="form-check-input" :value="c.categoryId" v-model="selectedCategoryIds"></td>
                                                    <td>{{ c.categoryName }}</td>
                                                </tr>
                                            </template>
                                            <template v-else>
                                                <tr v-for="p in filteredProducts" :key="p.productId">
                                                    <td><input type="checkbox" class="form-check-input" :value="p.productId" v-model="selectedMainProductIds"></td>
                                                    <td class="fw-bold">{{ p.productName }}</td>
                                                    <td><span class="badge" :class="p.productStock < 20 ? 'bg-danger' : 'bg-secondary'">{{ p.productStock }}</span></td>
                                                    <td><span class="text-danger fw-bold">${{ p.productPrice }}</span></td>
                                                </tr>
                                            </template>
                                        </tbody>
                                    </table>
                                </div>
                            </div>

                            <div v-show="currentStep === 3">
                                <div class="d-flex justify-content-between align-items-center bg-light p-3 rounded mb-3 border">
                                    <input v-if="formData.scopeType === 1" type="text" class="form-control form-control-sm" v-model="searchCategory" placeholder="🔍 搜尋副項..." style="width: 200px;">
                                    <input v-else type="text" class="form-control form-control-sm" v-model="searchProduct" placeholder="🔍 搜尋副項..." style="width: 200px;">
                                    <div class="d-flex align-items-center gap-4">
                                        <span class="fw-bold text-success">已勾選：{{ formData.scopeType === 1 ? selectedAddonCategoryIds.length : selectedAddonProductIds.length }} 件</span>
                                        
                                        <div class="form-check form-switch mb-0 d-flex align-items-center gap-2">
                                            <template v-if="formData.scopeType === 1">
                                                <input class="form-check-input mt-0" type="checkbox" v-model="showSelectedCategoryOnly" id="showSelCatAddon">
                                                <label class="form-check-label small mb-0" for="showSelCatAddon">只顯示已勾選</label>
                                            </template>
                                            <template v-else>
                                                <input class="form-check-input mt-0" type="checkbox" v-model="showSelectedProductOnly" id="showSelProdAddon">
                                                <label class="form-check-label small mb-0" for="showSelProdAddon">只顯示已勾選</label>
                                            </template>
                                        </div>
                                    </div>
                                </div>

                                <h6 class="fw-bold text-success mb-2 ps-1">🎁 副商品/副分類 (加購/贈品)</h6>

                                <div class="table-responsive border rounded" style="max-height: 350px;">
                                    <table class="table table-hover table-sm align-middle mb-0">
                                        <thead class="table-light position-sticky top-0">
                                            <tr><th style="width: 60px;">選取</th><th>名稱</th><th v-if="formData.scopeType===2">庫存</th><th v-if="formData.scopeType===2">價格</th></tr>
                                        </thead>
                                        <tbody>
                                            <template v-if="formData.scopeType === 1">
                                                <tr v-for="c in filteredCategories" :key="c.categoryId" :class="{'bg-light opacity-50': selectedCategoryIds.includes(c.categoryId) && formData.type === '4'}">
                                                    <td><input type="checkbox" class="form-check-input" :value="c.categoryId" v-model="selectedAddonCategoryIds" :disabled="selectedCategoryIds.includes(c.categoryId) && formData.type === '4' || (formData.type === '3')"></td>
                                                    <td>{{ c.categoryName }} <span v-if="selectedCategoryIds.includes(c.categoryId)" class="badge bg-secondary ms-2">[已選為主分類]</span></td>
                                                </tr>
                                            </template>
                                            <template v-else>
                                                <tr v-for="p in filteredProducts" :key="p.productId" :class="{'bg-light opacity-50': selectedMainProductIds.includes(p.productId)}">
                                                    <td><input type="checkbox" class="form-check-input" :value="p.productId" v-model="selectedAddonProductIds" :disabled="selectedMainProductIds.includes(p.productId)"></td>
                                                    <td>{{ p.productName }} <span v-if="selectedMainProductIds.includes(p.productId)" class="badge bg-secondary ms-2">[已選為主商品]</span></td>
                                                    <td><span class="badge" :class="p.productStock < 20 ? 'bg-danger' : 'bg-secondary'">{{ p.productStock }}</span></td>
                                                    <td><span class="text-danger fw-bold">${{ p.productPrice }}</span></td>
                                                </tr>
                                            </template>
                                        </tbody>
                                    </table>
                                </div>
                                <div v-if="formData.scopeType === 1 && formData.type === '3'" class="mt-2 text-muted small">💡 分類買 N 送 M 已鎖定同種類分類。</div>
                            </div>
                        </div>
                    </div>

                    <div class="modal-footer bg-light d-flex justify-content-end gap-3">
                        <button v-if="currentStep > 1" type="button" class="btn btn-outline-secondary px-4" @click="handlePrevStep">⬅ 上一步</button>
                        <button type="button" class="btn px-4" :class="isNextDisabled?'btn-secondary':'btn-success'" @click="handleNextStep" :disabled="isNextDisabled">
                            <span v-if="isSaving" class="spinner-border spinner-border-sm me-1"></span>{{ nextButtonText }}
                        </button>
                    </div>
                </div>
            </div>
        </div>

        <div class="modal fade" id="warnModal" tabindex="-1" data-bs-backdrop="static" style="z-index: 1060;">
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content border-warning shadow-lg">
                    <div class="modal-header bg-warning text-dark"><h5 class="modal-title fw-bold">⚠️ 變更適用範圍確認</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
                    <div class="modal-body">
                        <p class="text-danger fw-bold">這將會清空您剛才在「{{ formData.scopeType === 1 ? '指定分類清單' : '指定單品清單' }}」中的所有勾選紀錄！確定要切換範圍嗎？</p>
                    </div>
                    <div class="modal-footer"><button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">取消</button><button type="button" class="btn btn-danger" @click="confirmChangeScope">確定切換</button></div>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
.pointer-events-none { pointer-events: none; }
.nav-tabs .nav-link.active { color: #0d6efd; border-bottom: 2px solid #0d6efd; }
</style>