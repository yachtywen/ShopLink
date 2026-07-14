export interface UserDTO { id: number; nickName: string; icon: string }
export interface UserInfo { userId: number; city?: string; introduce?: string; fans?: number; followee?: number; gender?: boolean; birthday?: string; credits?: number; level?: boolean }
export interface LoginForm { phone: string; code: string }
export interface ShopType { id: number; name: string; icon: string; sort: number }
export interface Shop { id?: number; name?: string; typeId?: number; images?: string; area?: string; address?: string; x?: number; y?: number; avgPrice?: number; sold?: number; comments?: number; score?: number; openHours?: string; distance?: number; createTime?: string; updateTime?: string }
export interface Blog { id?: number; shopId?: number; userId?: number; icon?: string; name?: string; isLike?: boolean; title?: string; images?: string; content?: string; liked?: number; comments?: number; createTime?: string; updateTime?: string }
export interface Voucher { id?: number; shopId?: number; title?: string; subTitle?: string; rules?: string; payValue?: number; actualValue?: number; type?: number; status?: number; stock?: number; beginTime?: string; endTime?: string; createTime?: string; updateTime?: string }
export interface SeckillOrderSubmitResult { orderId: number; statusCode: number; statusText: string }
export interface VoucherOrderStatusResult extends SeckillOrderSubmitResult { payTime: string | null; createTime: string | null }
export interface ShopTypeQuery { typeId: number; current?: number; x?: number; y?: number }
