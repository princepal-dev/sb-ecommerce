package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Address;
import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.AddressDTO;
import com.ecommerce.project.payload.AddressResponse;
import com.ecommerce.project.repositories.AddressRepository;
import com.ecommerce.project.repositories.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressServiceImpl implements AddressService {
  @Autowired private ModelMapper modelMapper;

  @Autowired private AddressRepository addressRepository;

  @Autowired private UserRepository userRepository;

  @Override
  public AddressDTO createAddress(AddressDTO addressDTO, User user) {
    Address address = modelMapper.map(addressDTO, Address.class);

    List<Address> addressList = user.getAddresses();
    addressList.add(address);
    user.setAddresses(addressList);

    address.setUser(user);
    Address savedAddress = addressRepository.save(address);

    return modelMapper.map(savedAddress, AddressDTO.class);
  }

  @Override
  public AddressResponse getAllAddresses(
      Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
    Sort sortByAndOrder =
        sortOrder.equalsIgnoreCase("asc")
            ? Sort.by(sortBy).ascending()
            : Sort.by(sortBy).descending();

    Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
    Page<Address> addressPage = addressRepository.findAll(pageDetails);

    List<Address> addresses = addressPage.getContent();
    if (addresses.isEmpty()) throw new APIException("No addresses created till now.");

    List<AddressDTO> addressDTOS =
        addresses.stream().map(address -> modelMapper.map(address, AddressDTO.class)).toList();

    AddressResponse addressResponse = new AddressResponse();
    addressResponse.setContent(addressDTOS);
    addressResponse.setPageNumber(addressPage.getNumber());
    addressResponse.setPageSize(addressPage.getSize());
    addressResponse.setTotalElements(addressPage.getTotalElements());
    addressResponse.setTotalPages(addressPage.getTotalPages());
    addressResponse.setLastPage(addressPage.isLast());
    return addressResponse;
  }

  @Override
  public AddressDTO getAddressById(Long addressId) {
    Address address =
        addressRepository
            .findById(addressId)
            .orElseThrow(() -> new ResourceNotFoundException("Address", "addressId", addressId));
    return modelMapper.map(address, AddressDTO.class);
  }

  @Override
  public List<AddressDTO> getAddressByUser(User user) {
    List<Address> addresses = user.getAddresses();
    return addresses.stream().map(address -> modelMapper.map(address, AddressDTO.class)).toList();
  }

  @Override
  public AddressDTO updateAddressById(Long addressId, AddressDTO addressDTO) {
    Address addressFromDB =
        addressRepository
            .findById(addressId)
            .orElseThrow(() -> new ResourceNotFoundException("Address", "addressId", addressId));

    addressFromDB.setCity(addressDTO.getCity());
    addressFromDB.setPincode(addressDTO.getPincode());
    addressFromDB.setCountry(addressDTO.getCountry());
    addressFromDB.setState(addressDTO.getState());
    addressFromDB.setStreet(addressDTO.getStreet());
    addressFromDB.setBuildingName(addressDTO.getBuildingName());

    Address savedAddress = addressRepository.save(addressFromDB);

    User user = addressFromDB.getUser();
    user.getAddresses().removeIf(address -> address.getAddressId().equals(addressId));
    user.getAddresses().add(savedAddress);

    userRepository.save(user);

    return modelMapper.map(savedAddress, AddressDTO.class);
  }

  @Override
  public String deleteAddressById(Long addressId) {
    Address addressFromDB =
        addressRepository
            .findById(addressId)
            .orElseThrow(() -> new ResourceNotFoundException("Address", "addressId", addressId));

    User user = addressFromDB.getUser();
    user.getAddresses().removeIf(address -> address.getAddressId().equals(addressId));
    userRepository.save(user);

    addressRepository.delete(addressFromDB);

    return "Address deleted successfully with addressId: " + addressId;
  }
}
