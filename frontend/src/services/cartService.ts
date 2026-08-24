import { cart } from './api';

export const cartApi = {
  get: () => cart.get(),
  create: () => cart.create(),
  addItem: (productId: string, quantity: number) => cart.addItem(productId, quantity),
  updateItem: (itemId: string, quantity: number) => cart.updateItem(itemId, quantity),
  removeItem: (itemId: string) => cart.removeItem(itemId),
  clear: () => cart.clear(),
};
