import { createRouter, createWebHistory } from "vue-router";

const routes = [
  {
    path: "/",
    component: () => import("../views/Layout.vue"),
    meta: { requiresAuth: true },
    redirect: "/dashboard",
    children: [
      {
        path: "dashboard",
        name: "Dashboard",
        component: () => import("../views/Dashboard.vue"),
      },
      {
        path: "books",
        name: "BookList",
        component: () => import("../views/BookList.vue"),
      },
      {
        path: "blind-box",
        name: "BlindBox",
        component: () => import("../views/BlindBox.vue"),
      },
      {
        path: "auctions",
        name: "AuctionList",
        component: () => import("../views/AuctionList.vue"),
      },
      {
        path: "auction/:id",
        name: "AuctionDetail",
        component: () => import("../views/AuctionDetail.vue"),
      },
      {
        path: "ai-assistant",
        name: "AIAssistant",
        component: () => import("../views/AIAssistant.vue"),
      },
      {
        path: "borrows",
        name: "BorrowList",
        component: () => import("../views/BorrowList.vue"),
      },
      {
        path: "favorites",
        name: "MyFavorites",
        component: () => import("../views/MyFavorites.vue"),
      },
      {
        path: "profile",
        name: "Profile",
        component: () => import("../views/Profile.vue"),
      },
      {
        path: "stats",
        name: "StatsDashboard",
        component: () => import("../views/StatsDashboard.vue"),
      },
      {
        path: "users",
        name: "UserList",
        component: () => import("../views/UserList.vue"),
      },
      {
        path: "announcements",
        name: "AnnouncementManage",
        component: () => import("../views/AnnouncementManage.vue"),
      },
      {
        path: "reservations",
        name: "ReservationList",
        component: () => import("../views/ReservationList.vue"),
      },
      {
        path: "achievements",
        name: "Achievements",
        component: () => import("../views/Achievements.vue"),
      },
      {
        path: "booklists",
        name: "BookListShare",
        component: () => import("../views/BookListShare.vue"),
      },

      {
        path: "mystudy",
        name: "MyStudy",
        component: () => import("../views/MyStudy.vue"),
      },
      {
        path: "challenges",
        name: "Challenge",
        component: () => import("../views/Challenge.vue"),
      },
      {
        path: "messages",
        name: "Messages",
        component: () => import("../views/Messages.vue"),
      },
      {
        path: "groups",
        name: "ReadingGroups",
        component: () => import("../views/ReadingGroups.vue"),
      },
      {
        path: "cart",
        name: "ShoppingCart",
        component: () => import("../views/ShoppingCart.vue"),
      },
      {
        path: "orders",
        name: "OrderList",
        component: () => import("../views/OrderList.vue"),
      },
      {
        path: "payment",
        name: "PaymentPage",
        component: () => import("../views/PaymentPage.vue"),
      },
      {
        path: "addresses",
        name: "AddressManage",
        component: () => import("../views/AddressManage.vue"),
      },
      {
        path: "admin-orders",
        name: "AdminOrders",
        component: () => import("../views/AdminOrders.vue"),
      },
    ],
  },
  {
    path: "/login",
    name: "Login",
    component: () => import("../views/Login.vue"),
  },
  {
    path: "/register",
    name: "Register",
    component: () => import("../views/Register.vue"),
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// 路由守卫：未登录时跳转到登录页
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem("token");
  if (to.meta.requiresAuth && !token) {
    next("/login");
  } else {
    next();
  }
});

export default router;
