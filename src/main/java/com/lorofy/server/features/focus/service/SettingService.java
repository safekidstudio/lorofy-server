package com.lorofy.server.features.focus.service;

import com.lorofy.server.features.focus.entity.Setting;
import com.lorofy.server.features.focus.repository.SettingRepository;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SettingService {

    private final SettingRepository settingRepository;

    @Cacheable(value = "settings_all", key = "'all'")
    @Transactional(readOnly = true)
    public Map<String, String> getAllSettings() {
        return settingRepository.findAll().stream()
                .collect(Collectors.toMap(Setting::getKey, Setting::getValue));
    }

    @Cacheable(value = "settings", key = "#key")
    @Transactional(readOnly = true)
    public String getSetting(String key, String defaultValue) {
        return settingRepository.findById(key)
                .map(Setting::getValue)
                .orElse(defaultValue);
    }

    public double getDoubleSetting(String key, double defaultValue) {
        try {
            return Double.parseDouble(getSetting(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public int getIntSetting(String key, int defaultValue) {
        try {
            return Integer.parseInt(getSetting(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // 2. Sử dụng @Caching để xóa đồng thời cache của KEY đơn lẻ và cache ALL khi
    // cập nhật dữ liệu
    @Caching(evict = {
            @CacheEvict(value = "settings", key = "#key"),
            @CacheEvict(value = "settings_all", key = "'all'")
    })
    @Transactional
    public void updateSetting(String key, String value) {
        Setting setting = settingRepository.findById(key)
                .orElse(Setting.builder().key(key).value(value).description("System Config").build());
        setting.setValue(value);
        settingRepository.save(setting);
    }
}
