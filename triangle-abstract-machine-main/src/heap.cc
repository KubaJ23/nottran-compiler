//===-----------------------------------------------------------------------===//
//
// This file is part of tam-cpp, copyright (c) Ian Knight 2025.
//
// tam-cpp is free software: you can redistribute it and/or modify it under the
// terms of the GNU General Public License as published by the Free Software
// Foundation, either version 3 of the License, or (at your option) any later
// version.
//
// tam-cpp is distributed in the hope that it will be useful, but WITHOUT ANY
// WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
// A PARTICULAR PURPOSE. See the GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with tam-cpp. If not, see <https://www.gnu.org/licenses/>.
//
//===-----------------------------------------------------------------------===//
//
/// @file heap.cc
/// This file defines the `Allocate` and `free` methods of `TamEmulator`.
//
//===-----------------------------------------------------------------------===//

#include <assert.h>

#include <map>

#include "tam/error.h"
#include "tam/tam.h"

namespace tam {

/// Iterates through all existing free blocks in the heap to try and find one of
/// the correct size. If none is found, the heap is expanded.
TamAddr TamEmulator::Allocate(int n) {
    // try to find unallocated space inside heap
    for (auto block_iter = this->free_blocks_.begin(),
              iter_end = this->free_blocks_.end();
         block_iter != iter_end; ++block_iter) {
        assert(block_iter->first > this->registers_[HT]);

        if (block_iter->second < n)  // block not big enough
            continue;

        TamAddr block_start = block_iter->first;

        this->allocated_blocks_.emplace(block_start, n);
        if (block_iter->second > n)
            this->free_blocks_.emplace(block_start + n, block_iter->second - n);
        this->free_blocks_.erase(block_iter);

        return block_start;
    }

    // expand heap
    this->registers_[HT] -= n;
    if (this->registers_[HT] <= this->registers_[ST])
        throw RuntimeError(ExceptionKind::kHeapOverflow,
                           this->registers_[CP] - 1);

    this->allocated_blocks_.emplace(this->registers_[HT] + 1, n);
    return this->registers_[HT] + 1;
}

/// Attempts to locate an allocated block of the given address and size.
/// If the block was at the end of the heap then the heap is contracted,
/// otherwise the freed block is added to the list of available blocks.
void TamEmulator::Free(TamAddr addr, TamData size) {
    if (addr <= this->registers_[HT])
        throw RuntimeError(ExceptionKind::kDataAccessViolation,
                           this->registers_[CP] - 1);

    if (!this->allocated_blocks_.count(addr))
        throw RuntimeError(ExceptionKind::kDataAccessViolation,
                           this->registers_[CP] - 1);

    for (auto block_iter = this->allocated_blocks_.begin(),
              iter_end = this->allocated_blocks_.end();
         block_iter != iter_end; ++block_iter) {
        assert(block_iter->first > this->registers_[HT]);

        if (block_iter->first != addr)  // not the specified block
            continue;

        if (block_iter->second != size)  // block does not have specified size
            throw RuntimeError(ExceptionKind::kDataAccessViolation,
                               this->registers_[CP] - 1);

        if (addr == this->registers_[HT] + 1) {
            // block on top of heap, shrink heap
            this->registers_[HT] += block_iter->second;
        } else {
            // mark block as available
            this->free_blocks_.emplace(block_iter->first, block_iter->second);
        }
        this->allocated_blocks_.erase(block_iter);
        break;
    }
}

}  // namespace tam
