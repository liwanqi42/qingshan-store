package com.qingshan.service.impl;

import com.qingshan.constant.MessageConstant;
import com.qingshan.dto.SysMenuDTO;
import com.qingshan.entity.SysMenu;
import com.qingshan.exception.BaseException;
import com.qingshan.mapper.SysMenuMapper;
import com.qingshan.service.MenuService;
import com.qingshan.service.support.DefaultMenuData;
import com.qingshan.vo.SysMenuVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单展示配置服务，可访问范围仍由角色 menu_paths 控制
 */
@Service
public class MenuServiceImpl implements MenuService {

    private static final String NAME_PATTERN = "^[A-Za-z][A-Za-z0-9_]*$";

    private final SysMenuMapper menuMapper;

    public MenuServiceImpl(SysMenuMapper menuMapper) {
        this.menuMapper = menuMapper;
    }

    @Override
    public List<SysMenuVO> list() {
        return menuMapper.listAll().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public void save(SysMenuDTO menuDTO) {
        String name = checkName(menuDTO.getName());
        if (menuMapper.getByName(name) != null) {
            throw new BaseException(MessageConstant.MENU_NAME_EXISTS);
        }
        menuMapper.insert(SysMenu.builder()
                .name(name)
                .path(checkPath(menuDTO.getPath()))
                .title(checkTitle(menuDTO.getTitle()))
                .icon(checkIcon(menuDTO.getIcon()))
                .sort(menuDTO.getSort() == null ? 0 : menuDTO.getSort())
                .hidden(toFlag(menuDTO.getHidden(), 0))
                .more(toFlag(menuDTO.getMore(), 0))
                .build());
    }

    @Override
    public void update(SysMenuDTO menuDTO) {
        SysMenu existing = requireById(menuDTO.getId());
        if (StringUtils.hasText(menuDTO.getName())
                && !existing.getName().equals(menuDTO.getName().trim())) {
            throw new BaseException(MessageConstant.MENU_NAME_IMMUTABLE);
        }

        String path = menuDTO.getPath() == null ? null : checkPath(menuDTO.getPath());
        String title = menuDTO.getTitle() == null ? null : checkTitle(menuDTO.getTitle());
        String icon = menuDTO.getIcon() == null ? null : checkIcon(menuDTO.getIcon());
        Integer hidden = menuDTO.getHidden() == null ? null : toFlag(menuDTO.getHidden(), 0);
        Integer more = menuDTO.getMore() == null ? null : toFlag(menuDTO.getMore(), 0);
        if (path == null && title == null && icon == null
                && menuDTO.getSort() == null && hidden == null && more == null) {
            return;
        }

        menuMapper.update(SysMenu.builder()
                .id(existing.getId())
                .path(path)
                .title(title)
                .icon(icon)
                .sort(menuDTO.getSort())
                .hidden(hidden)
                .more(more)
                .build());
    }

    @Override
    public void delete(Long id) {
        requireById(id);
        menuMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void reset() {
        menuMapper.deleteAll();
        for (DefaultMenuData.MenuDefinition definition : DefaultMenuData.menus()) {
            menuMapper.insert(SysMenu.builder()
                    .name(definition.getName())
                    .path(definition.getPath())
                    .title(definition.getTitle())
                    .icon(definition.getIcon())
                    .sort(definition.getSort())
                    .hidden(definition.isHidden() ? 1 : 0)
                    .more(definition.isMore() ? 1 : 0)
                    .build());
        }
    }

    private SysMenuVO toVO(SysMenu menu) {
        return SysMenuVO.builder()
                .id(menu.getId())
                .name(menu.getName())
                .path(menu.getPath())
                .title(menu.getTitle())
                .icon(menu.getIcon())
                .sort(menu.getSort())
                .hidden(menu.getHidden())
                .more(menu.getMore())
                .build();
    }

    private SysMenu requireById(Long id) {
        SysMenu menu = id == null ? null : menuMapper.getById(id);
        if (menu == null) {
            throw new BaseException(MessageConstant.MENU_NOT_FOUND);
        }
        return menu;
    }

    private String checkName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BaseException(MessageConstant.MENU_NAME_REQUIRED);
        }
        String trimmed = name.trim();
        if (trimmed.length() > 64 || !trimmed.matches(NAME_PATTERN)) {
            throw new BaseException(MessageConstant.MENU_NAME_INVALID);
        }
        return trimmed;
    }

    private String checkTitle(String title) {
        if (!StringUtils.hasText(title)) {
            throw new BaseException(MessageConstant.MENU_TITLE_REQUIRED);
        }
        String trimmed = title.trim();
        if (trimmed.length() > 64) {
            throw new BaseException(MessageConstant.MENU_TITLE_TOO_LONG);
        }
        return trimmed;
    }

    private String checkPath(String path) {
        String trimmed = path == null ? null : path.trim();
        if (trimmed != null && trimmed.length() > 128) {
            throw new BaseException(MessageConstant.MENU_TEXT_TOO_LONG);
        }
        return trimmed;
    }

    private String checkIcon(String icon) {
        String trimmed = icon == null ? null : icon.trim();
        if (trimmed != null && trimmed.length() > 64) {
            throw new BaseException(MessageConstant.MENU_TEXT_TOO_LONG);
        }
        return trimmed;
    }

    private Integer toFlag(Boolean value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value ? 1 : 0;
    }
}
