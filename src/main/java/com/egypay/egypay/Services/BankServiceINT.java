package com.egypay.egypay.Services;

import com.egypay.egypay.Models.Entities.BankEntity;

public interface BankServiceINT {
    BankEntity findBankEntityBySwift(String swift);
    BankEntity findBankEntitiesByName(String Name);
    boolean save(BankEntity bankEntity);
}
