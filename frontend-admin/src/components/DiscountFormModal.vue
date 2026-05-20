<script setup>
import { ref, computed } from 'vue';
// ✨ 修改：引入封裝的 request
import request from '@/utils/request';
import * as bootstrap from 'bootstrap';

// ✨ 修改：移除 apiBaseUrl，統一在元件內使用相對路徑
const props = defineProps(['discountTypes']);
const emit = defineEmits(['saved']);

const currentStep = ref(1);
const tempScopeType = ref(null);
const isSaving = ref(false);

const selectedCategoryIds = ref([]);
const selectedMainProductIds = ref([]);
const selectedAddonProductIds = ref([]);
const selectedAddonCategoryIds = ref([]); 
// ✨ 新增：獨立控制副商品的範圍類型 (1:分類, 2:單品)
const addonScopeType = ref(1);
// ✨ 新增：動態計算當下副商品該看哪一個範圍 (只有 Type 4 可獨立切換，其餘綁定主範圍)
const currentAddonScopeType = computed(() => formData.value.type === '4' ? addonScopeType.value : formData.value.scopeType);




// ✨ 恢復搜尋與篩選狀態
const searchCategory = ref('');
const showSelectedCategoryOnly = ref(false);
const searchProduct = ref('');
const showSelectedProductOnly = ref(false);

// ✨ 修正：把 min 初始化為空字串，以觸發預覽防呆字
const formData = ref({
    id: null, scopeType: 1, name: '', status: 'active', type: '', 
    startDate: '', endDate: '', desc: '', min: '', val: '', buyQuantity: '', freeQuantity: '' 
});

const errors = ref({}); 

const productsList = ref([]);
const categoriesList = ref([]);

// ✨ 新增：用來綁定使用者選擇的活動標籤 ID
const selectedTagId = ref('');

// ✨ 新增：表單內彈出式快速建立標籤並自動選取功能
import Swal from 'sweetalert2';
const quickAddTagInForm = async () => {
    const { value: tagName } = await Swal.fire({
        // ✨ 修改：雙重保險！指定 SweetAlert2 掛載目標為本表單 Modal，強迫其在燈箱內部生成，徹底物理隔離焦點衝突
        target: document.getElementById('formModal'),
        title: '快速新增活動標籤',
        input: 'text',
        inputPlaceholder: '請輸入新標籤名稱（例如：端午特惠）',
        showCancelButton: true,
        confirmButtonText: '確定建立',
        cancelButtonText: '取消',
        inputValidator: (value) => {
            if (!value || !value.trim()) {
                return '標籤名稱不可為空！';
            }
        }
    });

    if (tagName) {
        try {
            const newTag = { categoryName: tagName.trim(), categoryType: 3, parentId: null };
            await request.post('/api/categories', newTag);
            Swal.fire({ icon: 'success', title: '新增成功', text: `標籤「${tagName}」已建立`, timer: 1500, showConfirmButton: false });
            
            // 重新刷新選單選項資料
            await fetchOptions();
            
            // 自動匹配剛剛建立成功的標籤物件，並將其設為當前下拉選單選中狀態
            const addedTag = categoriesList.value.find(c => c.categoryType === 3 && c.categoryName === tagName.trim());
            if (addedTag) {
                selectedTagId.value = addedTag.categoryId;
            }
        } catch (error) {
            console.error("表單內快速新增標籤失敗:", error);
            Swal.fire('失敗', '新增失敗，請檢查後端狀態', 'error');
        }
    }
};

// ✨ 新增/修改：優化 activityTags 邏輯，排除活動標籤大分類 (ID: 3 或名稱為優惠活動)
const activityTags = computed(() => {
    return categoriesList.value.filter(c => c.categoryType === 3 && c.categoryId !== 3 && c.categoryName !== '優惠活動');
});



const fetchOptions = async () => {
    try {
        // ✨ 修改：改用 request 並換成相對路徑
        const prodRes = await request.get('/api/products/list');
        productsList.value = prodRes.data.productList || [];
        // ✨ 修改：改用 request 並換成相對路徑
        const catRes = await request.get('/api/categories');
        categoriesList.value = catRes.data || [];
    } catch (error) { console.warn('資料取得失敗', error); }
};



// ✨ 新增：計算最後一步的索引值
const isThreeStep = computed(() => formData.value.type === '3' || formData.value.type === '4');
const lastStep = computed(() => isThreeStep.value ? 4 : 3);

const todayDate = computed(() => {
    const today = new Date();
    const yyyy = today.getFullYear();
    const mm = String(today.getMonth() + 1).padStart(2, '0');
    const dd = String(today.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
});

const isOngoing = computed(() => {
    if (!formData.value.id || !formData.value.startDate) return false;
    return formData.value.startDate <= todayDate.value;
});

const handleScopeChange = (newType) => {
    if (isOngoing.value) return;
    const hasSelected = selectedCategoryIds.value.length > 0 || selectedMainProductIds.value.length > 0 || 
                        selectedAddonProductIds.value.length > 0 || selectedAddonCategoryIds.value.length > 0;
    
    if (hasSelected) {
        tempScopeType.value = newType;
        new bootstrap.Modal(document.getElementById('warnModal')).show();
    }
};

const confirmChangeScope = () => {
    selectedCategoryIds.value = []; selectedMainProductIds.value = []; 
    selectedAddonProductIds.value = []; selectedAddonCategoryIds.value = [];
    bootstrap.Modal.getInstance(document.getElementById('warnModal')).hide();
};

const cancelChangeScope = () => {
    formData.value.scopeType = tempScopeType.value === 1 ? 2 : 1; 
    bootstrap.Modal.getInstance(document.getElementById('warnModal')).hide();
};

const checkStep1Valid = () => {
    errors.value = {};
    const f = formData.value;
    const t = f.type;
    let isValid = true;

    if (!f.name) { errors.value.name = '必填'; isValid = false; }
    else if (f.name.length > 10) { errors.value.name = '活動名稱不可超過 10 個字'; isValid = false; }
    if (!t) { errors.value.type = '必填'; isValid = false; }
    if (!f.startDate) { errors.value.startDate = '必填'; isValid = false; }
    if (!f.endDate) { errors.value.endDate = '必填'; isValid = false; }
    if (!f.desc) { errors.value.desc = '必填'; isValid = false; }
    
    if (f.min === null || f.min === '') { errors.value.min = '必填'; isValid = false; }
    else if (f.min < 0) { errors.value.min = '不得為負數'; isValid = false; }
    else if (t === '2' && f.min == 0) { errors.value.min = '滿額折門檻不可為0'; isValid = false; }

    if (t === '1') { 
        if (f.val === null || f.val === '') { errors.value.val = '必填'; isValid = false; }
        else if (f.val < 1 || f.val > 99) { errors.value.val = '請輸入 1 到 99 之間的數值'; isValid = false; }
    } else if (t === '2') { 
        if (f.val === null || f.val === '') { errors.value.val = '必填'; isValid = false; }
        else if (f.val <= 0) { errors.value.val = '不可輸入 0 或負數'; isValid = false; }
    } else if (t === '3') { 
        if (f.buyQuantity === null || f.buyQuantity === '') { errors.value.buyQuantity = '必填'; isValid = false; }
        else if (f.buyQuantity <= 0) { errors.value.buyQuantity = '不可輸入 0 或負數'; isValid = false; }
        if (f.freeQuantity === null || f.freeQuantity === '') { errors.value.freeQuantity = '必填'; isValid = false; }
        else if (f.freeQuantity <= 0) { errors.value.freeQuantity = '不可輸入 0 或負數'; isValid = false; }
    } else if (t === '4' || t === '5') { 
        if (f.buyQuantity === null || f.buyQuantity === '') { errors.value.buyQuantity = '必填'; isValid = false; }
        else if (f.buyQuantity <= 0) { errors.value.buyQuantity = '不可輸入 0 或負數'; isValid = false; }
        if (f.val === null || f.val === '') { errors.value.val = '必填'; isValid = false; }
        else if (f.val <= 0) { errors.value.val = '不可輸入 0 或負數'; isValid = false; }
    }

    return isValid;
};

// ✨ 修改：下一步按鈕文字邏輯，增加發布設定導向
const nextButtonText = computed(() => {
    if (currentStep.value === 1) return isThreeStep.value ? '下一步：選擇主項 ➔' : '下一步：選擇適用清單 ➔';
    if (currentStep.value === 2) return isThreeStep.value ? '下一步：選擇副項 ➔' : '下一步：發布設定 ➔';
    if (currentStep.value === 3 && isThreeStep.value) return '下一步：發布設定 ➔';
    return formData.value.id ? '儲存修改' : '儲存並發布';
});

// ✨ 修改：跳轉步驟邏輯，整合新增的發布設定頁籤
const handleNextStep = async () => {
    if (currentStep.value === 1) {
        if (!checkStep1Valid()) { alert("⚠️ 請檢查紅框處，資料尚未填寫完整或數值錯誤。"); return; }
        currentStep.value = 2;
    } else if (currentStep.value === 2) {
        const mainSelected = formData.value.scopeType === 1 ? selectedCategoryIds.value.length > 0 : selectedMainProductIds.value.length > 0;
        if (!mainSelected) { alert(`⚠️ 請至少選擇一個${formData.value.scopeType === 1 ? '分類' : '商品'}。`); return; }
        
        if (isThreeStep.value) {
            if (formData.value.scopeType === 1 && formData.value.type === '3') selectedAddonCategoryIds.value = [...selectedCategoryIds.value];
            if (formData.value.scopeType === 2 && formData.value.type === '3') selectedAddonProductIds.value = [...selectedMainProductIds.value];
            currentStep.value = 3; 
        } else { 
            currentStep.value = 3; // 直接進入發布設定 (無副商品時)
        }
    } else if (currentStep.value === 3 && isThreeStep.value) {
        const addonSelected = formData.value.scopeType === 1 ? selectedAddonCategoryIds.value.length > 0 : selectedAddonProductIds.value.length > 0;
        if (!addonSelected) { alert("⚠️ 請至少選擇一個副項。"); return; }
        currentStep.value = 4; // 進入發布設定 (有副商品時)
    } else {
        // ✨ 修改：最後一步提交前，驗證必選標籤
        if (!selectedTagId.value) { alert("⚠️ 請選擇此活動要發布到的活動標籤！"); return; }
        await saveActivity();
    }
};

// ✨ 修改：回退步驟邏輯
const handlePrevStep = () => { 
    if (currentStep.value === 4) currentStep.value = 3;
    else if (currentStep.value === 3) currentStep.value = isThreeStep.value ? 2 : 2; // 無論如何都回 step 2
    else currentStep.value--;
};

const saveActivity = async () => {
    isSaving.value = true;
    try {
        let finalValue = null; let finalBuyQty = null; let finalFreeQty = null;
        const typeCode = formData.value.type;
        if (typeCode === '1') { finalValue = formData.value.val / 100; } 
        else if (['2', '4', '5'].includes(typeCode)) { finalValue = formData.value.val; } 
        if (['3', '4', '5'].includes(typeCode)) { finalBuyQty = formData.value.buyQuantity; }
        if (typeCode === '3') { finalFreeQty = formData.value.freeQuantity; }

        const payload = {
            discount: {
                discountId: formData.value.id || null, discountName: formData.value.name,
                scopeType: formData.value.scopeType, status: formData.value.status,
                startDate: formData.value.startDate, endDate: formData.value.endDate,
                discountDescription: formData.value.desc, minimumPurchaseAmount: formData.value.min || 0,
                discountValue: finalValue, buyQuantity: finalBuyQty, freeQuantity: finalFreeQty,
                discountType: { discountTypeId: parseInt(formData.value.type) }
            },
            categoryIds: formData.value.scopeType === 1 ? selectedCategoryIds.value : [],
            mainProductIds: formData.value.scopeType === 2 ? selectedMainProductIds.value : [],
           // ✨ 修改：脫離主範圍限制，改用 currentAddonScopeType 判斷陣列傳遞
            addonProductIds: (currentAddonScopeType.value === 2 && isThreeStep.value) ? selectedAddonProductIds.value : [],
            addonCategoryIds: (currentAddonScopeType.value === 1 && isThreeStep.value) ? selectedAddonCategoryIds.value : [],
            // ✨ 修改：將選取的標籤 ID 送入 Payload
            tagCategoryId: selectedTagId.value ? parseInt(selectedTagId.value) : null
        };
        
        if (formData.value.id) await request.put(`/api/discounts/${formData.value.id}`, payload);
        else await request.post(`/api/discounts/save`, payload);
        emit('saved');
        bootstrap.Modal.getInstance(document.getElementById('formModal')).hide();
    } catch (error) { alert("儲存失敗"); } finally { isSaving.value = false; }
};

const showFormModal = () => {
    const el = document.getElementById('formModal');
    const modal = bootstrap.Modal.getInstance(el) || new bootstrap.Modal(el);
    modal.show();
};

defineExpose({
    openAdd() {
        formData.value = { id: null, scopeType: 1, name: '', status: 'active', type: '', startDate: '', endDate: '', desc: '', min: '', val: '', buyQuantity: '', freeQuantity: '' };
        currentStep.value = 1; errors.value = {};
        selectedCategoryIds.value = []; selectedMainProductIds.value = []; 
        selectedAddonProductIds.value = []; selectedAddonCategoryIds.value = [];
        
        // ✨ 新增：新增模式清空標籤
        selectedTagId.value = '';

        searchCategory.value = ''; searchProduct.value = '';
        showSelectedCategoryOnly.value = false; showSelectedProductOnly.value = false;
        fetchOptions(); showFormModal();
    },
    openEdit(item) {
        errors.value = {}; currentStep.value = 1;
        let displayValue = item.discountValue;
        if (item.discountType?.discountTypeId === 1) displayValue = Math.round(item.discountValue * 100); 

        formData.value = {
            id: item.discountId, scopeType: item.scopeType || 1, name: item.discountName,
            status: item.status, type: item.discountType ? item.discountType.discountTypeId.toString() : '',
            startDate: item.startDate, endDate: item.endDate, desc: item.discountDescription,
            min: item.minimumPurchaseAmount, val: displayValue, buyQuantity: item.buyQuantity, freeQuantity: item.freeQuantity
        };
        selectedCategoryIds.value = item.discountCategories?.filter(c => c.categoryRole === 'Main').map(c => c.category?.categoryId) || [];
        selectedAddonCategoryIds.value = item.discountCategories?.filter(c => c.categoryRole === 'Addon').map(c => c.category?.categoryId) || [];
        selectedMainProductIds.value = item.discountProducts?.filter(p => p.productRole === 'Main').map(p => p.product?.productId) || [];
        selectedAddonProductIds.value = item.discountProducts?.filter(p => p.productRole === 'Addon').map(p => p.product?.productId) || [];
        
        // ✨ 新增：修改模式抓取角色為 Tag 的標籤 ID
        const tagCategory = item.discountCategories?.find(c => c.categoryRole === 'Tag');
        selectedTagId.value = tagCategory ? tagCategory.category?.categoryId : '';

        searchCategory.value = ''; searchProduct.value = '';
        showSelectedCategoryOnly.value = false; showSelectedProductOnly.value = false;
        fetchOptions(); showFormModal();
    }
});

// ✨ 修改：優化 filteredCategories 邏輯，排除活動標籤
const filteredCategories = computed(() => {
    // ✨ 新增：過濾邏輯最前端增加 categoryType !== 3 條件，確保標籤不會混入一般選單
    let res = categoriesList.value.filter(c => c.categoryType !== 3);
    
    if (currentStep.value === 2 && showSelectedCategoryOnly.value) res = res.filter(c => selectedCategoryIds.value.includes(c.categoryId));
    if (currentStep.value === 3 && showSelectedCategoryOnly.value && isThreeStep.value) res = res.filter(c => selectedAddonCategoryIds.value.includes(c.categoryId));
    if (searchCategory.value) res = res.filter(c => c.categoryName.includes(searchCategory.value));
    return res;
});

const filteredProducts = computed(() => {
    let res = productsList.value;
    if (currentStep.value === 2 && showSelectedProductOnly.value) res = res.filter(p => selectedMainProductIds.value.includes(p.productId));
    if (currentStep.value === 3 && showSelectedProductOnly.value && isThreeStep.value) res = res.filter(p => selectedAddonProductIds.value.includes(p.productId));
    if (searchProduct.value) res = res.filter(p => p.productName.includes(searchProduct.value));
    return res;
});
</script>

<template>
    <div>
       <div class="modal fade" id="formModal" data-bs-backdrop="static">
            <div class="modal-dialog modal-lg modal-dialog-scrollable">
                <div class="modal-content border-0 shadow">
                    <div class="modal-header bg-light border-bottom-0">
                        <h5 class="modal-title fw-bold">✨ {{ formData.id ? '修改' : '新增' }}優惠活動</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body p-0">
                        <ul class="nav nav-tabs px-4 pt-3 bg-light border-0">
                            <li class="nav-item">
                                <button class="nav-link fw-bold" :class="{active: currentStep===1}" @click="currentStep=1">📜 活動規則</button>
                            </li>
                            <li class="nav-item">
                                <button class="nav-link fw-bold" :class="{active: currentStep===2, 'pointer-events-none opacity-50': !formData.id && currentStep < 2}" @click="currentStep=2">📦 指定對象</button>
                            </li>
                            <li class="nav-item" v-if="isThreeStep">
                                <button class="nav-link fw-bold" :class="{active: currentStep===3, 'pointer-events-none opacity-50': !formData.id && currentStep < 3}" @click="currentStep=3">🎁 副商品</button>
                            </li>
                            <li class="nav-item">
                                <button class="nav-link fw-bold" :class="{active: currentStep === lastStep, 'pointer-events-none opacity-50': !formData.id && currentStep < lastStep}" @click="currentStep = lastStep">📢 發布設定</button>
                            </li>
                        </ul>

                        <div class="tab-content p-4 bg-white">
                            <div v-show="currentStep === 1">
                                <div v-if="isOngoing" class="alert alert-warning py-2 mb-3 small fw-bold">🔒 活動進行中，核心規則已鎖定。</div>
                                <form @submit.prevent>
                                    <div class="mb-3 p-3 bg-light border rounded shadow-sm">
                                        <label class="form-label fw-bold mb-2 small text-secondary">適用範圍 <span class="text-danger">*</span></label>
                                        <div class="d-flex gap-4">
                                            <div class="form-check">
                                                <input class="form-check-input" type="radio" :value="1" v-model="formData.scopeType" @change="handleScopeChange(1)" :disabled="isOngoing" id="sc1">
                                                <label class="form-check-label fw-bold small" for="sc1">指定分類清單</label>
                                            </div>
                                            <div class="form-check">
                                                <input class="form-check-input" type="radio" :value="2" v-model="formData.scopeType" @change="handleScopeChange(2)" :disabled="isOngoing" id="sc2">
                                                <label class="form-check-label fw-bold small" for="sc2">指定單品清單</label>
                                            </div>
                                        </div>
                                    </div>

                                    <div class="row mb-3">
                                        <div class="col-md-3">
                                            <label class="form-label text-muted small fw-bold">狀態</label>
                                            <select class="form-select border-2" v-model="formData.status"><option value="active">啟用</option><option value="inactive">停用</option></select>
                                        </div>
                                        <div class="col-md-9">
                                            <label class="form-label text-muted small fw-bold">活動名稱 <span class="text-danger">*</span></label>
                                            <input type="text" class="form-control border-2" v-model.trim="formData.name" :class="{'is-invalid': errors.name}" placeholder="請輸入活動名稱" maxlength="10">
                                            <div class="invalid-feedback">{{ errors.name }}</div>
                                        </div>
                                    </div>

                                    <div class="mb-3">
                                        <label class="form-label text-muted small fw-bold">折扣類型 <span class="text-danger">*</span></label>
                                        <select class="form-select border-2" v-model="formData.type" :class="{'is-invalid': errors.type}" :disabled="isOngoing">
                                            <option value="" disabled>請選擇折扣類型</option>
                                            <option v-for="t in props.discountTypes" :key="t.discountTypeId" :value="t.discountTypeId.toString()">{{ t.discountTypeName }}</option>
                                        </select>
                                        <div class="invalid-feedback">{{ errors.type }}</div>
                                    </div>

                                    <div class="p-3 bg-light rounded mb-3 border shadow-sm" v-if="formData.type">
                                        <div class="row" v-if="['1', '2'].includes(formData.type)">
                                            <div class="col-12">
                                                <label class="form-label fw-bold small">折扣值 <span class="text-danger">*</span></label>
                                                <div class="input-group">
                                                    <input type="number" step="1" class="form-control border-2" v-model.number="formData.val" :class="{'is-invalid': errors.val}" :disabled="isOngoing" :placeholder="formData.type === '1' ? '打 85 折輸入 85' : '折扣 100 輸入 100'">
                                                    <span class="input-group-text bg-white border-2 border-start-0 small">{{ formData.type === '1' ? '%' : '元' }}</span>
                                                    <div class="invalid-feedback">{{ errors.val }}</div>
                                                </div>
                                            </div>
                                        </div>
                                        <div class="row mt-2" v-if="['3', '4', '5'].includes(formData.type)">
                                            <div class="col-6">
                                                <label class="form-label fw-bold small">{{ formData.type === '4' ? '主項需滿 N 件' : (formData.type === '5' ? '任選 N 件' : '買 N 件') }} *</label>
                                                <input type="number" class="form-control border-2" v-model.number="formData.buyQuantity" :class="{'is-invalid': errors.buyQuantity}" :disabled="isOngoing" placeholder="數量">
                                                <div class="invalid-feedback">{{ errors.buyQuantity }}</div>
                                            </div>
                                            <div class="col-6" v-if="formData.type === '3'">
                                                <label class="form-label fw-bold small">送 M 件 *</label>
                                                <input type="number" class="form-control border-2" v-model.number="formData.freeQuantity" :class="{'is-invalid': errors.freeQuantity}" :disabled="isOngoing" placeholder="數量">
                                                <div class="invalid-feedback">{{ errors.freeQuantity }}</div>
                                            </div>
                                            <div class="col-6" v-if="['4', '5'].includes(formData.type)">
                                                <label class="form-label fw-bold small">{{ formData.type === '4' ? '副商品加購價' : '組合總價' }} *</label>
                                                <div class="input-group">
                                                    <input type="number" class="form-control border-2" v-model.number="formData.val" :class="{'is-invalid': errors.val}" :disabled="isOngoing" placeholder="金額">
                                                    <span class="input-group-text bg-white border-2 border-start-0 small">元</span>
                                                    <div class="invalid-feedback">{{ errors.val }}</div>
                                                </div>
                                            </div>
                                        </div>
                                    </div>

                                    <div class="row mb-3">
                                        <div class="col-md-4">
                                            <label class="form-label text-muted small fw-bold">門檻金額 *</label>
                                            <input type="number" class="form-control border-2" v-model.number="formData.min" :class="{'is-invalid': errors.min}" :disabled="isOngoing" placeholder="最低門檻為0">
                                            <div class="invalid-feedback">{{ errors.min }}</div>
                                        </div>
                                        <div class="col-md-4">
                                            <label class="form-label text-muted small fw-bold">開始日期 *</label>
                                            <input type="date" class="form-control border-2" v-model="formData.startDate" :class="{'is-invalid': errors.startDate}" :min="todayDate" :disabled="isOngoing">
                                            <div class="invalid-feedback">{{ errors.startDate }}</div>
                                        </div>
                                        <div class="col-md-4">
                                            <label class="form-label text-muted small fw-bold">結束日期 *</label>
                                            <input type="date" class="form-control border-2" v-model="formData.endDate" :class="{'is-invalid': errors.endDate}" :min="formData.startDate">
                                            <div class="invalid-feedback">{{ errors.endDate }}</div>
                                        </div>
                                    </div>
                                    <div class="mb-0">
                                        <label class="form-label text-muted small fw-bold">活動描述 *</label>
                                        <textarea class="form-control border-2" rows="3" v-model="formData.desc" :class="{'is-invalid': errors.desc}" placeholder="請輸入活動詳細說明..."></textarea>
                                        <div class="invalid-feedback">{{ errors.desc }}</div>
                                    </div>
                                </form>
                            </div>

                            <div v-show="currentStep === 2">
                                <div class="d-flex mb-3 align-items-center border-bottom pb-2 gap-3">
                                    <h6 class="fw-bold mb-0 text-nowrap">📦 {{ formData.scopeType === 1 ? '主分類選擇' : '主商品選擇' }}</h6>
                                    
                                    <input type="text" class="form-control form-control-sm border-2" v-model="searchCategory" v-if="formData.scopeType === 1" placeholder="🔍 搜尋名稱..." style="width: 140px;">
                                    <input type="text" class="form-control form-control-sm border-2" v-model="searchProduct" v-else placeholder="🔍 搜尋名稱..." style="width: 140px;">
                                    
                                    <div class="form-check form-switch mb-0 ms-auto">
                                        <input class="form-check-input" type="checkbox" v-model="showSelectedCategoryOnly" v-if="formData.scopeType===1" id="showSelCat1">
                                        <input class="form-check-input" type="checkbox" v-model="showSelectedProductOnly" v-else id="showSelProd1">
                                        <label class="form-check-label small text-muted mt-1" :for="formData.scopeType===1 ? 'showSelCat1' : 'showSelProd1'">只顯示已勾選</label>
                                    </div>
                                    </div>
                                <div class="table-responsive border rounded" style="max-height: 350px;">
                                    <table class="table table-hover align-middle mb-0">
                                        <thead class="table-light"><tr><th style="width: 50px;"></th><th>名稱</th><th v-if="formData.scopeType===2">庫存</th><th v-if="formData.scopeType===2">售價</th></tr></thead>
                                        <tbody>
                                            <template v-if="formData.scopeType === 1">
                                                <tr v-for="c in filteredCategories" :key="c.categoryId">
                                                    <td>
                                                        <input v-if="formData.type === '3'" type="radio" name="catRadioGroup" class="form-check-input ms-2" :value="c.categoryId" :checked="selectedCategoryIds.includes(c.categoryId)" @change="selectedCategoryIds = [c.categoryId]" :disabled="isOngoing">
                                                        <input v-else type="checkbox" class="form-check-input ms-2" :value="c.categoryId" v-model="selectedCategoryIds" :disabled="isOngoing">
                                                    </td>
                                                    <td>{{ c.categoryName }}</td>
                                                </tr>
                                            </template>
                                            <template v-else>
                                                <tr v-for="p in filteredProducts" :key="p.productId">
                                                    <td><input type="checkbox" class="form-check-input ms-2" :value="p.productId" v-model="selectedMainProductIds" :disabled="isOngoing"></td>
                                                    <td>{{ p.productName }}</td>
                                                    <td>{{ p.productStock }}</td>
                                                    <td>$ {{ p.productPrice }}</td>
                                                </tr>
                                            </template>
                                        </tbody>
                                    </table>
                                </div>
                            </div>

                            <div v-show="currentStep === 3 && isThreeStep">
                                <div class="d-flex mb-3 align-items-center border-bottom pb-2 gap-3">
                                    <h6 class="fw-bold mb-0 text-success text-nowrap">🎁 副商品選擇</h6>
                                    
                                   
                                    
                                    <input type="text" class="form-control form-control-sm border-2" v-model="searchCategory" v-if="formData.scopeType === 1" placeholder="🔍 搜尋名稱..." style="width: 140px;">
                                    <input type="text" class="form-control form-control-sm border-2" v-model="searchProduct" v-else placeholder="🔍 搜尋名稱..." style="width: 140px;">
                                     <span v-if="formData.scopeType === 1 && formData.type === '3'" class="text-danger small fw-bold text-nowrap">⚠️ 主副商品必須為同一分類</span>
                                    <div class="form-check form-switch mb-0 ms-auto">
                                        <input class="form-check-input" type="checkbox" v-model="showSelectedCategoryOnly" v-if="formData.scopeType===1" id="showSelCat2">
                                        <input class="form-check-input" type="checkbox" v-model="showSelectedProductOnly" v-else id="showSelProd2">
                                        <label class="form-check-label small text-muted mt-1" :for="formData.scopeType===1 ? 'showSelCat2' : 'showSelProd2'">只顯示已勾選</label>
                                    </div>
                                    </div>
                                <div class="table-responsive border rounded" style="max-height: 350px;">
                                    <table class="table table-hover align-middle mb-0">
                                        <thead class="table-light"><tr><th style="width: 50px;"></th><th>名稱</th><th v-if="formData.scopeType===2">庫存</th><th v-if="formData.scopeType===2">售價</th></tr></thead>
                                        <tbody>
                                            <template v-if="formData.scopeType === 1">
                                                <tr v-for="c in filteredCategories" :key="c.categoryId" :class="{'bg-light opacity-50': selectedCategoryIds.includes(c.categoryId) || (formData.type==='3' && !selectedCategoryIds.includes(c.categoryId))}">
                                                    <td><input type="checkbox" class="form-check-input ms-2" :value="c.categoryId" v-model="selectedAddonCategoryIds" :disabled="isOngoing || formData.type==='3' || (formData.type==='4' && selectedCategoryIds.includes(c.categoryId))"></td>
                                                    <td>{{ c.categoryName }} <span v-if="selectedCategoryIds.includes(c.categoryId)" class="badge bg-secondary ms-2 small">[已選為主分類]</span></td>
                                                </tr>
                                            </template>
                                            <template v-else>
                                                <tr v-for="p in filteredProducts" :key="p.productId" :class="{'bg-light opacity-50': selectedMainProductIds.includes(p.productId) || (formData.type==='3' && !selectedMainProductIds.includes(p.productId))}">
                                                    <td><input type="checkbox" class="form-check-input ms-2" :value="p.productId" v-model="selectedAddonProductIds" :disabled="isOngoing || formData.type==='3' || (formData.type==='4' && selectedMainProductIds.includes(p.productId))"></td>
                                                    <td>{{ p.productName }} <span v-if="selectedMainProductIds.includes(p.productId)" class="badge bg-secondary ms-2 small">[已選為主商品]</span></td>
                                                    <td>{{ p.productStock }}</td>
                                                    <td>$ {{ p.productPrice }}</td>
                                                </tr>
                                            </template>
                                        </tbody>
                                    </table>
                                </div>
                            </div>

                            <div v-show="currentStep === lastStep">
    <div class="p-4 bg-light border rounded shadow-sm border-start border-4 border-warning">
        <h5 class="fw-bold text-dark mb-3"><i class="fas fa-bullhorn me-2 text-warning"></i> 📢 活動發布與掛載設定</h5>
        
        <div class="mb-4">
            <label class="form-label fw-bold text-secondary small">選擇前端活動標籤 <span class="text-danger">*</span></label>
            <p class="small text-muted mb-2">請選擇此折扣活動要掛載到使用者介面的哪一個活動分頁（此為必選項目）。</p>
            
            <div class="d-flex gap-2">
                <select class="form-select border-2 py-3" v-model="selectedTagId" :disabled="isOngoing">
                    <option value="" disabled>-- 請選擇掛載標籤 --</option>
                    <option v-for="tag in activityTags" :key="tag.categoryId" :value="tag.categoryId">
                        📌 {{ tag.categoryName }} (ID: {{ tag.categoryId }})
                    </option>
                </select>
                <button type="button" class="btn btn-outline-primary px-3 text-nowrap fw-bold shadow-sm" @click="quickAddTagInForm" :disabled="isOngoing">
                    ➕ 快速新增
                </button>
            </div>
        </div>

        <div class="alert alert-info py-2 mb-0 small">
            <i class="fas fa-info-circle me-1"></i> 提示：若清單中沒有需要的標籤，可以直接點擊右側的「快速新增」按鈕建立。
        </div>
    </div>
</div>
                        </div>
                    </div>

                    <div class="modal-footer bg-light border-top-0 gap-3">
                        <button v-if="currentStep === 1" type="button" class="btn btn-outline-secondary px-4 fw-bold shadow-sm" data-bs-dismiss="modal">取消</button>
                        <button v-if="currentStep > 1" class="btn btn-outline-success px-4 fw-bold shadow-sm" @click="handlePrevStep">⬅ 上一步</button>
                        <button class="btn btn-success px-4 fw-bold shadow-sm" @click="handleNextStep">
                            <span v-if="isSaving" class="spinner-border spinner-border-sm me-2"></span>{{ nextButtonText }}
                        </button>
                    </div>
                </div>
            </div>
        </div>

        <div class="modal fade" id="warnModal" tabindex="-1" data-bs-backdrop="static" style="z-index: 1060;">
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content border-warning shadow-lg">
                    <div class="modal-header bg-warning text-dark"><h5 class="modal-title fw-bold">⚠️ 變更適用範圍確認</h5><button type="button" class="btn-close" @click="cancelChangeScope"></button></div>
                    <div class="modal-body">
                        <p class="text-danger fw-bold mb-0">這將會清空您剛才在「{{ tempScopeType === 1 ? '指定單品清單' : '指定分類清單' }}」中的所有勾選紀錄！確定要切換範圍嗎？</p>
                    </div>
                    <div class="modal-footer"><button type="button" class="btn btn-outline-secondary" @click="cancelChangeScope">取消</button><button type="button" class="btn btn-danger" @click="confirmChangeScope">確定切換</button></div>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
.pointer-events-none { pointer-events: none; }
.nav-tabs .nav-link { color: #6c757d; border: none; padding: 10px 20px; transition: 0.3s; }
.nav-tabs .nav-link.active { color: #0d6efd; border-bottom: 3px solid #0d6efd; background: transparent; }
.form-select, .form-control { border-radius: 8px; padding: 10px; }
.btn { border-radius: 8px; transition: 0.3s; }
.invalid-feedback { font-weight: bold; }
</style>