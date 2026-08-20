package com.qingshan.service.impl;

import com.qingshan.constant.MessageConstant;
import com.qingshan.context.BaseContext;
import com.qingshan.entity.AddressBook;
import com.qingshan.exception.AddressBookBusinessException;
import com.qingshan.exception.UserNotLoginException;
import com.qingshan.mapper.AddressBookMapper;
import com.qingshan.service.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class AddressBookServiceImpl implements AddressBookService {

    @Autowired
    private AddressBookMapper addressBookMapper;

    /**
     * 条件查询
     *
     * @param addressBook
     * @return
     */
    @Override
    public List<AddressBook> list(AddressBook addressBook) {
        return addressBookMapper.list(addressBook);
    }

    /**
     * 新增地址
     *
     * @param addressBook
     */
    @Override
    public void save(AddressBook addressBook) {
        addressBook.setUserId(getCurrentUserId());
        addressBook.setIsDefault(0);
        addressBookMapper.insert(addressBook);
    }

    /**
     * 根据id查询
     *
     * @param id
     * @return
     */
    @Override
    public AddressBook getById(Long id) {
        return getOwnedAddress(id);
    }

    /**
     * 根据id修改地址
     *
     * @param addressBook
     */
    @Override
    public void update(AddressBook addressBook) {
        if (addressBook.getId() == null) {
            throw new AddressBookBusinessException("地址不存在");
        }
        // 只能修改自己的地址
        getOwnedAddress(addressBook.getId());
        addressBookMapper.update(addressBook);
    }

    /**
     * 设置默认地址
     *
     * @param addressBook
     */
    @Override
    @Transactional
    public void setDefault(AddressBook addressBook) {
        if (addressBook.getId() == null) {
            throw new AddressBookBusinessException("地址不存在");
        }
        // 只能将自己的地址设为默认地址
        getOwnedAddress(addressBook.getId());

        //1、将当前用户的所有地址修改为非默认地址 update address_book set is_default = ? where user_id = ?
        addressBook.setIsDefault(0);
        addressBook.setUserId(getCurrentUserId());
        addressBookMapper.updateIsDefaultByUserId(addressBook);

        //2、将当前地址改为默认地址 update address_book set is_default = ? where id = ?
        addressBook.setIsDefault(1);
        addressBookMapper.update(addressBook);
    }

    /**
     * 根据id删除地址
     *
     * @param id
     */
    @Override
    public void deleteById(Long id) {
        // 只能删除自己的地址
        getOwnedAddress(id);
        addressBookMapper.deleteById(id);
    }

    /**
     * 查询属于当前登录用户的地址
     *
     * @param id 地址id
     * @return 地址信息
     */
    private AddressBook getOwnedAddress(Long id) {
        AddressBook addressBook = id == null ? null : addressBookMapper.getById(id);
        if (addressBook == null || !Objects.equals(addressBook.getUserId(), getCurrentUserId())) {
            throw new AddressBookBusinessException("地址不存在");
        }
        return addressBook;
    }

    /**
     * 获取当前登录用户id
     *
     * @return 用户id
     */
    private Long getCurrentUserId() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new UserNotLoginException(MessageConstant.USER_NOT_LOGIN);
        }
        return userId;
    }

}
