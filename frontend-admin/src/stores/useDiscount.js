import { ref, computed } from 'vue';
import axios from 'axios';

export function useDiscount() {
    const API_BASE_URL = 'http://localhost:8082/api/discounts';

    const discounts = ref([]);
    const discountTypesList = ref([]);
    const loading = ref(false);

    const searchActivityName = ref('');
    const statusFilter = ref('all');
    const scopeFilter = ref('all');
    const typeFilter = ref('all');

    const getComputedStatus = (status, startStr, endStr) => {
        if (status === 'deleted') return 'deleted';
        if (status === 'inactive') return 'inactive';
        if (!startStr || !endStr) return 'active';
        const now = new Date();
        const start = new Date(startStr.replace(/-/g, '/')); start.setHours(0, 0, 0, 0);
        const end = new Date(endStr.replace(/-/g, '/')); end.setHours(23, 59, 59, 999);
        if (now < start) {
            const hoursDiff = (start.getTime() - now.getTime()) / (1000 * 60 * 60);
            return hoursDiff <= 24 ? 'upcoming' : 'not_started';
        }
        if (now >= start && now <= end) return 'active';
        return 'expired';
    };

    const getStatusBadge = (item) => {
        const status = getComputedStatus(item.status, item.startDate, item.endDate);
        switch (status) {
            case 'deleted': return { text: '🗑️ 已刪除', class: 'bg-dark text-white' };
            case 'inactive': return { text: '🔴 已停用', class: 'bg-danger' };
            case 'not_started': return { text: '🔵 尚未開始', class: 'bg-info text-dark' };
            case 'upcoming': return { text: '🟡 即將開始', class: 'bg-warning text-dark' };
            case 'active': return { text: '🟢 進行中', class: 'bg-success' };
            case 'expired': return { text: '⚪ 已結束', class: 'bg-secondary' };
            default: return { text: '未知', class: 'bg-secondary' };
        }
    };

    const fetchDiscounts = async () => {
        loading.value = true;
        try {
            const response = await axios.get(API_BASE_URL);
            discounts.value = response.data;
        } catch (error) { console.error("無法取得活動:", error); }
        finally { loading.value = false; }
    };

    const fetchDiscountTypes = async () => {
        try {
            const response = await axios.get(`${API_BASE_URL}/types`);
            discountTypesList.value = response.data;
        } catch (error) { console.error("無法取得類型:", error); }
    };

    // ✨ 修正：乾淨打包 Payload，保證 PUT 成功，並移除危險的 DELETE Fallback
    const deleteActivity = async (item) => {
        if (item.status === 'active') {
            alert("無法刪除進行中的活動！請先將狀態改為停用。");
            return;
        }
        if (!confirm('確定要刪除這筆活動嗎？(系統將執行軟刪除隱藏此筆資料)')) return;

        try {
            const catIds = item.discountCategories?.filter(c => c.categoryRole === 'Main').map(c => c.category?.categoryId) || [];
            const addCatIds = item.discountCategories?.filter(c => c.categoryRole === 'Addon').map(c => c.category?.categoryId) || [];
            const prodIds = item.discountProducts?.filter(p => p.productRole === 'Main').map(p => p.product?.productId) || [];
            const addProdIds = item.discountProducts?.filter(p => p.productRole === 'Addon').map(p => p.product?.productId) || [];

            // 乾淨重構 discount 物件，只給後端需要的欄位，防止解析錯誤
            const cleanDiscount = {
                discountId: item.discountId,
                discountName: item.discountName,
                scopeType: item.scopeType,
                status: 'deleted', // 強制設為已刪除
                startDate: item.startDate,
                endDate: item.endDate,
                discountDescription: item.discountDescription,
                minimumPurchaseAmount: item.minimumPurchaseAmount,
                discountValue: item.discountValue,
                buyQuantity: item.buyQuantity,
                freeQuantity: item.freeQuantity,
                discountType: { discountTypeId: item.discountType?.discountTypeId }
            };

            const payload = {
                discount: cleanDiscount,
                categoryIds: catIds, mainProductIds: prodIds, addonProductIds: addProdIds, addonCategoryIds: addCatIds
            };

            await axios.put(`${API_BASE_URL}/${item.discountId}`, payload);
            alert('已成功移至垃圾桶 (軟刪除)！');
            fetchDiscounts();
        } catch (error) {
            // 如果出錯，直接報錯讓開發者知道，而不是偷偷把它硬刪除
            console.error("軟刪除失敗:", error);
            alert('軟刪除失敗，請檢查 F12 控制台日誌！');
        }
    };

    const filteredDiscounts = computed(() => {
        let res = discounts.value;
        if (statusFilter.value === 'deleted') {
            res = res.filter(d => d.status === 'deleted');
        } else {
            res = res.filter(d => d.status !== 'deleted');
            if (statusFilter.value !== 'all') {
                res = res.filter(d => getComputedStatus(d.status, d.startDate, d.endDate) === statusFilter.value);
            }
        }
        if (scopeFilter.value !== 'all') res = res.filter(d => d.scopeType?.toString() === scopeFilter.value);
        if (typeFilter.value !== 'all') res = res.filter(d => d.discountType?.discountTypeId.toString() === typeFilter.value);
        if (searchActivityName.value) res = res.filter(d => d.discountName?.includes(searchActivityName.value));
        return res;
    });

    return {
        API_BASE_URL, discounts, discountTypesList, loading,
        searchActivityName, statusFilter, scopeFilter, typeFilter,
        fetchDiscounts, fetchDiscountTypes, deleteActivity,
        filteredDiscounts, getStatusBadge
    };
}