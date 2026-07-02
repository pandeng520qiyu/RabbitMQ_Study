/**
 * api.js — 前端 API 请求封装
 *
 * 网关地址：http://localhost:8080
 * 所有请求自动携带 Authorization: ******
 */

const API_BASE = 'http://localhost:8080';
const TOKEN_KEY = 'rmq_token';
const USER_KEY  = 'rmq_user';

// ────────────────────────────────────────────────────────────────
//  Token 管理
// ────────────────────────────────────────────────────────────────

const Auth = {
  saveToken(token, user) {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  },

  getToken() {
    return localStorage.getItem(TOKEN_KEY);
  },

  getUser() {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  },

  clear() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  },

  isLoggedIn() {
    return !!this.getToken();
  },
};

// ────────────────────────────────────────────────────────────────
//  基础 fetch 封装
// ────────────────────────────────────────────────────────────────

async function request(method, path, body) {
  const headers = { 'Content-Type': 'application/json' };
  const token = Auth.getToken();
  if (token) {
    headers['Authorization'] = 'Bearer ' + token;
  }

  const res = await fetch(API_BASE + path, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });

  // 401 → 跳转登录
  if (res.status === 401) {
    Auth.clear();
    window.location.href = 'index.html';
    return;
  }

  const json = await res.json();
  if (json.code !== 200) {
    throw new Error(json.message || '请求失败');
  }
  return json.data;
}

const get  = (path)        => request('GET',    path);
const post = (path, body)  => request('POST',   path, body);
const put  = (path, body)  => request('PUT',    path, body);
const del  = (path)        => request('DELETE', path);

// ────────────────────────────────────────────────────────────────
//  认证 API
// ────────────────────────────────────────────────────────────────

const AuthAPI = {
  async login(username, password) {
    const data = await post('/api/auth/login', { username, password });
    Auth.saveToken(data.token, data.userInfo);
    return data;
  },

  async register(username, password, email) {
    return post('/api/auth/register', { username, password, email });
  },

  logout() {
    Auth.clear();
    window.location.href = 'index.html';
  },
};

// ────────────────────────────────────────────────────────────────
//  订单 API
// ────────────────────────────────────────────────────────────────

const OrderAPI = {
  list()               { return get('/api/orders'); },
  get(id)              { return get(`/api/orders/${id}`); },
  create(order)        { return post('/api/orders', order); },
  createTtl(order)     { return post('/api/orders/ttl', order); },
  pay(id)              { return put(`/api/orders/${id}/pay`); },
  cancel(id)           { return put(`/api/orders/${id}/cancel`); },
};

// ────────────────────────────────────────────────────────────────
//  UI 工具
// ────────────────────────────────────────────────────────────────

/** 显示 Toast 通知 */
function showToast(message, type = 'info') {
  let container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    document.body.appendChild(container);
  }
  const toast = document.createElement('div');
  const icons = { success: '✅', error: '❌', info: 'ℹ️' };
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `<span>${icons[type] || ''}</span><span>${message}</span>`;
  container.appendChild(toast);
  setTimeout(() => toast.remove(), 3500);
}

/** 显示/隐藏加载状态 */
function setLoading(btn, loading) {
  if (!btn) return;
  btn.disabled = loading;
  if (loading) {
    btn._originalText = btn.innerHTML;
    btn.innerHTML = '<span class="spinner"></span> 处理中...';
  } else {
    btn.innerHTML = btn._originalText || btn.innerHTML;
  }
}

/** 将订单状态转成中文徽章 */
function statusBadge(status) {
  const map = {
    PENDING:    ['gray',    '待支付'],
    PAID:       ['success', '已支付'],
    SHIPPED:    ['info',    '已发货'],
    COMPLETED:  ['success', '已完成'],
    CANCELLED:  ['danger',  '已取消'],
    REFUNDING:  ['warning', '退款中'],
  };
  const [color, label] = map[status] || ['gray', status];
  return `<span class="badge badge-${color}">${label}</span>`;
}

/** 格式化日期 */
function fmtDate(str) {
  if (!str) return '-';
  return str.replace('T', ' ').slice(0, 16);
}
