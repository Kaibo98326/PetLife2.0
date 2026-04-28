import { defineStore } from 'pinia'
import { jwtDecode } from 'jwt-decode'

export const useEmployeeStore = defineStore('employee', {
  state: () => ({
    token: null,
    employeeId: null,
    empName: null,
    role: null
  }),
  actions: {
    login(token) {
      this.token = token
      localStorage.setItem('employeeToken', token)

      const decoded = jwtDecode(token)
      this.employeeId = parseInt(decoded.sub)
      this.empName = decoded.empName
      this.role = decoded.role
    },
    logout() {
      this.token = null
      this.employeeId = null
      this.empName = null
      this.role = null
      localStorage.removeItem('employeeToken')
    }
  }
})