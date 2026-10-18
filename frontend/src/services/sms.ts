import { api } from './api';
import type { SmsConfiguration, CreateSmsConfigurationRequest } from '@/types/sms';
import type { SmsMessage, CreateSmsMessageRequest } from '@/types/sms';
import type { SmsTemplate, CreateSmsTemplateRequest } from '@/types/sms';
import type { PageResponse } from '@/types/api';

export async function getSmsConfigurations(page = 0, size = 20): Promise<PageResponse<SmsConfiguration>> {
  const { data } = await api.get(`/platform/sms/configurations?page=${page}&size=${size}`);
  return data.data;
}

export async function getSmsConfigurationsBySchool(schoolId: number, page = 0, size = 20): Promise<PageResponse<SmsConfiguration>> {
  const { data } = await api.get(`/platform/sms/configurations/school/${schoolId}?page=${page}&size=${size}`);
  return data.data;
}

export async function getSmsConfiguration(id: number) {
  const { data } = await api.get(`/platform/sms/configurations/${id}`);
  return data.data;
}

export async function createSmsConfiguration(payload: CreateSmsConfigurationRequest) {
  const { data } = await api.post('/platform/sms/configurations', payload);
  return data.data;
}

export async function updateSmsConfiguration(id: number, payload: CreateSmsConfigurationRequest) {
  const { data } = await api.put(`/platform/sms/configurations/${id}`, payload);
  return data.data;
}

export async function deleteSmsConfiguration(id: number) {
  const { data } = await api.delete(`/platform/sms/configurations/${id}`);
  return data;
}

export async function getSmsMessages(page = 0, size = 20): Promise<PageResponse<SmsMessage>> {
  const { data } = await api.get(`/school/sms/messages?page=${page}&size=${size}`);
  return data.data;
}

export async function sendSms(payload: CreateSmsMessageRequest) {
  const { data } = await api.post('/school/sms/messages', payload);
  return data.data;
}

export async function sendBulkSms(payload: CreateSmsMessageRequest) {
  const { data } = await api.post('/school/sms/messages/bulk', payload);
  return data.data;
}

export async function getSmsTemplates(page = 0, size = 20): Promise<PageResponse<SmsTemplate>> {
  const { data } = await api.get(`/school/sms/templates?page=${page}&size=${size}`);
  return data.data;
}

export async function getSmsTemplate(id: number) {
  const { data } = await api.get(`/school/sms/templates/${id}`);
  return data.data;
}

export async function createSmsTemplate(payload: CreateSmsTemplateRequest) {
  const { data } = await api.post('/school/sms/templates', payload);
  return data.data;
}

export async function updateSmsTemplate(id: number, payload: CreateSmsTemplateRequest) {
  const { data } = await api.put(`/school/sms/templates/${id}`, payload);
  return data.data;
}

export async function deleteSmsTemplate(id: number) {
  const { data } = await api.delete(`/school/sms/templates/${id}`);
  return data;
}
