import { defineStore } from 'pinia'
import request from '@/utils/request'

export const useProductStore = defineStore('product', {
  state: () => ({
    lowStockCount: 0
  }),
  actions: {
    async fetchLowStockCount() {
      try {
        const res = await request.get('/api/products/low-stock-count')
        this.lowStockCount = res.data
      } catch (error) {
        console.error('Failed to fetch low stock count:', error)
      }
    }
  }
})
