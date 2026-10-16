package com.qingshan.service.impl;

import com.qingshan.constant.MessageConstant;
import com.qingshan.constant.StatusConstant;
import com.qingshan.dto.DictItemDTO;
import com.qingshan.dto.DictTypeDTO;
import com.qingshan.entity.SysDictItem;
import com.qingshan.entity.SysDictType;
import com.qingshan.exception.BaseException;
import com.qingshan.mapper.SysDictItemMapper;
import com.qingshan.mapper.SysDictTypeMapper;
import com.qingshan.service.DictService;
import com.qingshan.service.support.DefaultDictData;
import com.qingshan.vo.DictItemVO;
import com.qingshan.vo.DictTypeVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 字典服务：管理端维护，用户端只读
 */
@Service
public class DictServiceImpl implements DictService {

    private static final String TYPE_PATTERN = "^[a-z][a-z0-9_]*$";
    private static final Set<String> TAG_TYPES =
            new HashSet<>(Arrays.asList("", "success", "info", "warning", "danger"));

    private final SysDictTypeMapper dictTypeMapper;
    private final SysDictItemMapper dictItemMapper;

    public DictServiceImpl(SysDictTypeMapper dictTypeMapper, SysDictItemMapper dictItemMapper) {
        this.dictTypeMapper = dictTypeMapper;
        this.dictItemMapper = dictItemMapper;
    }

    @Override
    public List<DictTypeVO> listForAdmin() {
        return toVOList(dictTypeMapper.listAll(), dictItemMapper.listAll());
    }

    @Override
    public List<DictTypeVO> listForUser() {
        return toVOList(dictTypeMapper.listEnabled(), dictItemMapper.listEnabled());
    }

    @Override
    public void saveType(DictTypeDTO dictTypeDTO) {
        String type = checkTypeCode(dictTypeDTO.getType());
        String name = checkTypeName(dictTypeDTO.getName());
        if (dictTypeMapper.getByType(type) != null) {
            throw new BaseException(MessageConstant.DICT_TYPE_EXISTS);
        }

        SysDictType dictType = SysDictType.builder()
                .type(type)
                .name(name)
                .status(dictTypeDTO.getStatus() == null ? StatusConstant.ENABLE : dictTypeDTO.getStatus())
                .sort(dictTypeDTO.getSort() == null ? 0 : dictTypeDTO.getSort())
                .isSystem(DefaultDictData.isSystem(type) ? 1 : 0)
                .build();
        dictTypeMapper.insert(dictType);
    }

    @Override
    public void updateType(DictTypeDTO dictTypeDTO) {
        SysDictType existing = requireType(dictTypeDTO.getId());
        if (StringUtils.hasText(dictTypeDTO.getType())
                && !existing.getType().equals(dictTypeDTO.getType().trim())) {
            throw new BaseException(MessageConstant.DICT_TYPE_IMMUTABLE);
        }

        String name = dictTypeDTO.getName() == null ? null : checkTypeName(dictTypeDTO.getName());
        if (name == null && dictTypeDTO.getStatus() == null && dictTypeDTO.getSort() == null) {
            return;
        }

        dictTypeMapper.update(SysDictType.builder()
                .id(existing.getId())
                .name(name)
                .status(dictTypeDTO.getStatus())
                .sort(dictTypeDTO.getSort())
                .build());
    }

    @Override
    @Transactional
    public void deleteType(Long id) {
        SysDictType existing = requireType(id);
        if (Integer.valueOf(1).equals(existing.getIsSystem())) {
            throw new BaseException(MessageConstant.DICT_SYSTEM_PROTECTED);
        }
        dictItemMapper.deleteByTypeId(id);
        dictTypeMapper.deleteById(id);
    }

    @Override
    public void saveItem(DictItemDTO dictItemDTO) {
        SysDictType dictType = requireType(dictItemDTO.getTypeId());
        String label = checkItemLabel(dictItemDTO.getLabel());
        String value = checkItemValue(dictItemDTO.getValue());
        String tagType = normalizeTagType(dictItemDTO.getTagType());
        if (dictItemMapper.getByTypeIdAndValue(dictType.getId(), value) != null) {
            throw new BaseException(MessageConstant.DICT_ITEM_EXISTS);
        }

        dictItemMapper.insert(SysDictItem.builder()
                .typeId(dictType.getId())
                .label(label)
                .value(value)
                .tagType(tagType)
                .sort(dictItemDTO.getSort() == null ? 0 : dictItemDTO.getSort())
                .status(dictItemDTO.getStatus() == null ? StatusConstant.ENABLE : dictItemDTO.getStatus())
                .build());
    }

    @Override
    public void updateItem(DictItemDTO dictItemDTO) {
        SysDictItem existing = requireItem(dictItemDTO.getId());
        if (dictItemDTO.getValue() != null
                && !existing.getValue().equals(dictItemDTO.getValue().trim())) {
            throw new BaseException(MessageConstant.DICT_ITEM_VALUE_IMMUTABLE);
        }

        String label = dictItemDTO.getLabel() == null ? null : checkItemLabel(dictItemDTO.getLabel());
        String tagType = dictItemDTO.getTagType() == null ? null : normalizeTagType(dictItemDTO.getTagType());
        if (label == null && tagType == null
                && dictItemDTO.getSort() == null && dictItemDTO.getStatus() == null) {
            return;
        }

        dictItemMapper.update(SysDictItem.builder()
                .id(existing.getId())
                .label(label)
                .tagType(tagType)
                .sort(dictItemDTO.getSort())
                .status(dictItemDTO.getStatus())
                .build());
    }

    @Override
    public void deleteItem(Long id) {
        requireItem(id);
        dictItemMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void reset() {
        dictItemMapper.deleteAll();
        dictTypeMapper.deleteAll();

        for (DefaultDictData.Type definition : DefaultDictData.types()) {
            SysDictType dictType = SysDictType.builder()
                    .type(definition.getCode())
                    .name(definition.getName())
                    .status(StatusConstant.ENABLE)
                    .sort(definition.getSort())
                    .isSystem(1)
                    .build();
            dictTypeMapper.insert(dictType);

            for (DefaultDictData.Item itemDefinition : definition.getItems()) {
                dictItemMapper.insert(SysDictItem.builder()
                        .typeId(dictType.getId())
                        .label(itemDefinition.getLabel())
                        .value(itemDefinition.getValue())
                        .tagType(itemDefinition.getTagType())
                        .sort(itemDefinition.getSort())
                        .status(StatusConstant.ENABLE)
                        .build());
            }
        }
    }

    private List<DictTypeVO> toVOList(List<SysDictType> types, List<SysDictItem> items) {
        Map<Long, List<SysDictItem>> itemsByType = items.stream()
                .collect(Collectors.groupingBy(SysDictItem::getTypeId));
        List<DictTypeVO> result = new ArrayList<>(types.size());
        for (SysDictType type : types) {
            List<SysDictItem> typeItems = itemsByType.getOrDefault(type.getId(), Collections.emptyList());
            result.add(DictTypeVO.builder()
                    .id(type.getId())
                    .type(type.getType())
                    .name(type.getName())
                    .status(type.getStatus())
                    .sort(type.getSort())
                    .isSystem(type.getIsSystem())
                    .items(typeItems.stream().map(this::toItemVO).collect(Collectors.toList()))
                    .build());
        }
        return result;
    }

    private DictItemVO toItemVO(SysDictItem item) {
        return DictItemVO.builder()
                .id(item.getId())
                .label(item.getLabel())
                .value(item.getValue())
                .tagType(item.getTagType())
                .sort(item.getSort())
                .status(item.getStatus())
                .build();
    }

    private SysDictType requireType(Long id) {
        SysDictType type = id == null ? null : dictTypeMapper.getById(id);
        if (type == null) {
            throw new BaseException(MessageConstant.DICT_TYPE_NOT_FOUND);
        }
        return type;
    }

    private SysDictItem requireItem(Long id) {
        SysDictItem item = id == null ? null : dictItemMapper.getById(id);
        if (item == null) {
            throw new BaseException(MessageConstant.DICT_ITEM_NOT_FOUND);
        }
        return item;
    }

    private String checkTypeCode(String type) {
        if (!StringUtils.hasText(type)) {
            throw new BaseException(MessageConstant.DICT_TYPE_REQUIRED);
        }
        String trimmed = type.trim();
        if (!trimmed.matches(TYPE_PATTERN)) {
            throw new BaseException(MessageConstant.DICT_TYPE_INVALID);
        }
        if (trimmed.length() > 64) {
            throw new BaseException(MessageConstant.DICT_TYPE_INVALID);
        }
        return trimmed;
    }

    private String checkTypeName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BaseException(MessageConstant.DICT_NAME_REQUIRED);
        }
        String trimmed = name.trim();
        if (trimmed.length() > 64) {
            throw new BaseException(MessageConstant.DICT_NAME_TOO_LONG);
        }
        return trimmed;
    }

    private String checkItemLabel(String label) {
        if (!StringUtils.hasText(label)) {
            throw new BaseException(MessageConstant.DICT_ITEM_LABEL_REQUIRED);
        }
        String trimmed = label.trim();
        if (trimmed.length() > 64) {
            throw new BaseException(MessageConstant.DICT_ITEM_LABEL_TOO_LONG);
        }
        return trimmed;
    }

    private String checkItemValue(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BaseException(MessageConstant.DICT_ITEM_VALUE_REQUIRED);
        }
        String trimmed = value.trim();
        if (trimmed.length() > 64) {
            throw new BaseException(MessageConstant.DICT_ITEM_VALUE_TOO_LONG);
        }
        return trimmed;
    }

    private String normalizeTagType(String tagType) {
        String normalized = tagType == null ? "" : tagType.trim();
        if (!TAG_TYPES.contains(normalized)) {
            throw new BaseException(MessageConstant.DICT_TAG_TYPE_INVALID);
        }
        return normalized;
    }
}
