import { ref, computed } from 'vue';
import axios from 'axios';

export function useDiscount() {
    const API_BASE_URL = 'http://localhost:8082/api/discounts';
    
    // --- 1. 狀態管理 ---
    const discounts = ref([]);
    const discountTypesList = ref([]);
    const loading = ref(false);

    // 篩選與搜尋
    const searchActivityName = ref('');
    const statusFilter = ref('all');
    const scopeFilter = ref('all');
    const typeFilter = ref('all');

    // --- 2. 工具邏輯：狀態計算與 Badge ---
    const getComputedStatus = (status, startStr, endStr) => {
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
        switch(status) {
            case 'inactive': return { text: '🔴 已停用', class: 'bg-danger' };
            case 'not_started': return { text: '🔵 尚未開始', class: 'bg-info text-dark' };
            case 'upcoming': return { text: '🟡 即將開始', class: 'bg-warning text-dark' };
            case 'active': return { text: '🟢 進行中', class: 'bg-success' };
            case 'expired': return { text: '⚪ 已結束', class: 'bg-secondary' };
            default: return { text: '未知', class: 'bg-secondary' };
        }
    };

    // --- 3. API 呼叫 ---
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

    const deleteActivity = async (item) => {
        if (item.status === 'active') {
            alert("無法刪除進行中的活動！請先將狀態改為停用。");
            return;
        }
        if (!confirm('確定要刪除這筆活動嗎？(系統將執行軟刪除隱藏此筆資料)')) return;
        
        try {
            const payload = {
                discount: { ...item, status: 'deleted', discountType: { discountTypeId: item.discountType?.discountTypeId } },
                categoryIds: [], mainProductIds: [], addonProductIds: []
            };
            await axios.put(`${API_BASE_URL}/${item.discountId}`, payload);
            fetchDiscounts();
        } catch (error) {
            try {
                await axios.delete(`${API_BASE_URL}/${item.discountId}`);
                fetchDiscounts();
            } catch (e) { alert('刪除失敗'); }
        }
    };

    // --- 4. 計算屬性：列表篩選 ---
    const filteredDiscounts = computed(() => {
        let res = discounts.value.filter(d => d.status !== 'deleted');
        if (scopeFilter.value !== 'all') res = res.filter(d => d.scopeType?.toString() === scopeFilter.value);
        if (typeFilter.value !== 'all') res = res.filter(d => d.discountType?.discountTypeId.toString() === typeFilter.value);
        if (statusFilter.value !== 'all') res = res.filter(d => getComputedStatus(d.status, d.startDate, d.endDate) === statusFilter.value);
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